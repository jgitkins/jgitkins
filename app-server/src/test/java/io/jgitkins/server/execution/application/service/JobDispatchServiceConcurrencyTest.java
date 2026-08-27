package io.jgitkins.server.execution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.JGitkinsServerApplication;
import io.jgitkins.server.execution.application.contract.command.DispatchJobCommand;
import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.application.contract.result.JobDispatchResult;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import io.jgitkins.server.execution.domain.repository.JobRepository;

import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import io.jgitkins.server.shared.domain.model.vo.SequenceNumber;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = JGitkinsServerApplication.class, properties = {
        "spring.datasource.hikari.jdbc-url=jdbc:h2:mem:dispatch;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.datasource.hikari.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.username=sa",
        "spring.datasource.hikari.password=",
        "spring.datasource.hikari.maximum-pool-size=8",
        "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
        "spring.sql.init.mode=never",
        "server.port=18081", "grpc.server.port=19091",
        "REST_PORT=8080", "GRPC_PORT=9090", "BARE_PATH=/tmp", "SERVICE_HOST=localhost",
        "REST_SCHEME=http", "JGITKINS_JWT_SECRET=test-secret"
})
class JobDispatchServiceConcurrencyTest {
    @Autowired DataSource dataSource;
    @Autowired JobDispatchService service;
    @MockBean JobDispatchQueryPort queryPort;
    @MockBean io.jgitkins.server.execution.application.support.RunnerDispatchContextResolver resolver;
    @MockBean io.jgitkins.server.execution.application.support.JobDispatchResultAssembler assembler;
    @MockBean io.jgitkins.server.execution.application.port.out.CloneUrlPort cloneUrlPort;
    @SpyBean JobHistoryEntityMbgMapper historyMapper;
    // Spied through the port, not the concrete adapter. Task 2.73 put JobRepository behind the
    // persistence selector, so the container's bean is declared as JobRepository and there is no
    // longer a bean whose type is JobRepositoryAdapter. Spying the concrete class silently produced a
    // different object from the one the service injects: the doAnswer below never fired and the test
    // failed on a latch timeout that pointed nowhere near the cause.
    //
    // The verify on historyMapper.selectLatestHistoryForUpdate keeps this test specific to the MyBatis
    // provider, which is the default the selector resolves to. The JPA path's equivalent guarantee is
    // ExecutionJpaTransactionTest#preservesCompareBeforeAppend.
    @SpyBean JobRepository repositoryAdapter;
    ExecutorService executor;

    @BeforeEach void schemaAndSnapshot() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS JOB (ID BIGINT AUTO_INCREMENT PRIMARY KEY, REPOSITORY_ID BIGINT, COMMIT_HASH VARCHAR(255), BRANCH_NAME VARCHAR(255), TRIGGERED_BY BIGINT, CREATED_AT TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS JOB_HISTORY (ID BIGINT AUTO_INCREMENT PRIMARY KEY, JOB_ID BIGINT, RUNNER_ID BIGINT, STATUS VARCHAR(50), LOG_PATH VARCHAR(255), STARTED_AT TIMESTAMP, FINISHED_AT TIMESTAMP, CREATED_AT TIMESTAMP)");
        jdbc.update("DELETE FROM JOB_HISTORY"); jdbc.update("DELETE FROM JOB");
        jdbc.update("INSERT INTO JOB (ID,REPOSITORY_ID,COMMIT_HASH,BRANCH_NAME,TRIGGERED_BY,CREATED_AT) VALUES (1,2,'abc123','main',3,?)", LocalDateTime.of(2026,8,19,12,0));
        jdbc.update("INSERT INTO JOB_HISTORY (ID,JOB_ID,RUNNER_ID,STATUS,CREATED_AT) VALUES (10,1,NULL,'PENDING',?)", LocalDateTime.of(2026,8,19,12,0));
    }

    @AfterEach void cleanup() throws Exception { if (executor != null) { executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS); } }

    @Test void dispatch_usesRealTransactionalProxyAndRowLockToAllowExactlyOneWinner() throws Exception {
        RunnerDispatchContext context = new RunnerDispatchContext(7L, JobDispatchScope.GLOBAL, null);
        DispatchableJob first = snapshot(); DispatchableJob second = snapshot();
        CountDownLatch fetchedTwo = new CountDownLatch(2), releaseQueries = new CountDownLatch(1);
        CountDownLatch firstAppendReturned = new CountDownLatch(1), secondAppendEntered = new CountDownLatch(1), releaseFirstAppend = new CountDownLatch(1);
        AtomicBoolean firstAppend = new AtomicBoolean();

        when(resolver.resolve("token")).thenReturn(Optional.of(context));
        when(queryPort.fetchNextJob(any())).thenAnswer(invocation -> { fetchedTwo.countDown(); await(fetchedTwo); await(releaseQueries); return Optional.of(fetchedTwo.getCount() == 0 ? (Thread.currentThread().getId() % 2 == 0 ? first : second) : first); });
        when(assembler.assemble(any(), any(), any(), any(), any())).thenReturn(new JobDispatchResult(1L, 11L, 7L, 2L, null, "abc123", "main", 3L, LocalDateTime.now(), "url"));
        doAnswer(invocation -> {
            Optional<Long> result = (Optional<Long>) invocation.callRealMethod();
            if (firstAppend.compareAndSet(false, true)) {
                firstAppendReturned.countDown();
                await(releaseFirstAppend);
            } else {
                secondAppendEntered.countDown();
            }
            return result;
        }).when(repositoryAdapter).appendHistoryIfCurrent(any(), any());

        executor = Executors.newFixedThreadPool(2);
        Future<Optional<JobDispatchResult>> a = executor.submit(() -> service.dispatch(new DispatchJobCommand("token")));
        Future<Optional<JobDispatchResult>> b = executor.submit(() -> service.dispatch(new DispatchJobCommand("token")));
        assertThat(fetchedTwo.await(10, TimeUnit.SECONDS)).isTrue(); releaseQueries.countDown();
        assertThat(firstAppendReturned.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(secondAppendEntered.await(1, TimeUnit.SECONDS)).isFalse();
        releaseFirstAppend.countDown();

        Optional<JobDispatchResult> ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
        assertThat(List.of(ra, rb).stream().filter(Optional::isPresent)).hasSize(1);
        assertThat(List.of(ra, rb).stream().filter(Optional::isEmpty)).hasSize(1);
        assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM JOB_HISTORY WHERE JOB_ID=1", Integer.class)).isEqualTo(2);
        assertThat(new JdbcTemplate(dataSource).queryForObject("SELECT ID FROM JOB_HISTORY WHERE JOB_ID=1 AND STATUS='IN_PROGRESS'", Long.class)).isNotNull();
        verify(historyMapper, times(2)).selectLatestHistoryForUpdate(1L);
        verify(repositoryAdapter, times(2)).appendHistoryIfCurrent(any(), any());
    }

    private DispatchableJob snapshot() {
        LocalDateTime created = LocalDateTime.of(2026,8,19,12,0);
        JobHistory pending = JobHistory.reconstruct(JobHistoryId.of("10"), JobId.of("1"), SequenceNumber.first(), null, JobStatus.PENDING, ExecutionSystemActor.SYSTEM, created);
        Job job = Job.reconstruct(JobId.of("1"), ExecutionRepositoryId.of(2L), CommitHash.of("abc1234"), BranchName.of("main"), ExecutionActorId.of(3L), created, List.of(pending));
        return new DispatchableJob(1L, job, null, "/repo");
    }
    private void await(CountDownLatch latch) { try { if (!latch.await(20, TimeUnit.SECONDS)) throw new AssertionError("concurrency latch timed out"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); } }
}
