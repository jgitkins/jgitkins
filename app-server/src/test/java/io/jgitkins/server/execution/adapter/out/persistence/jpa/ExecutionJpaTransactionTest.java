package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.domain.aggregate.Job;
import io.jgitkins.server.execution.domain.entity.JobHistory;
import io.jgitkins.server.execution.domain.vo.ExecutionActorId;
import io.jgitkins.server.execution.domain.vo.ExecutionRepositoryId;
import io.jgitkins.server.execution.domain.vo.ExecutionSystemActor;
import io.jgitkins.server.execution.domain.vo.JobHistoryId;
import io.jgitkins.server.execution.domain.vo.JobId;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.execution.domain.vo.RunnerId;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import io.jgitkins.server.shared.domain.model.vo.SequenceNumber;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The compare-before-append must stay a compare-and-set under JPA, not a read-then-write.
 *
 * <p>Two dispatchers can observe the same job at the same instant. Both will decide to append the same
 * transition, and both will call {@code appendHistoryIfCurrent} with the same expected-previous
 * history. Exactly one may win. If both win, the job is dispatched twice — it runs twice, against the
 * same commit, with two runners believing they own it.
 *
 * <p>What makes exactly one win is the {@code FOR UPDATE} on the latest-history read, which serializes
 * the two transactions so the loser re-reads a latest row that no longer matches what it expected.
 * That is InnoDB behaviour, not Hibernate's, which is why this test needs a real database: an in-memory
 * provider would let both transactions through and report the invariant as satisfied.
 *
 * <p>Negative control: removing {@code @Lock(PESSIMISTIC_WRITE)} from
 * {@code JobHistoryJpaRepository.lockLatestForJob} makes this test fail on the row count.
 */
class ExecutionJpaTransactionTest {

    private static final String PACKAGES = "io.jgitkins.server.execution.adapter.out.persistence.jpa";

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private EntityManagerFactory emf;
    private JdbcTemplate jdbc;
    private ExecutorService executor;
    private String commitHash;
    private Long jobId;
    private JobHistory pending;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(
                dataSource, "execution-jpa-transaction", PACKAGES);
        emf = factoryBean.getObject();
        jdbc = new JdbcTemplate(dataSource);
        executor = Executors.newFixedThreadPool(2);
        commitHash = "jpa73tx" + Long.toString(System.nanoTime(), 36);

        LocalDateTime created = LocalDateTime.now().withNano(0);
        newTransactions().executeWithoutResult(status -> newAdapter().save(Job.reconstruct(
                JobId.of("0"), ExecutionRepositoryId.of(7311L), CommitHash.of(commitHash),
                BranchName.of("main"), ExecutionActorId.of(7312L), created,
                List.of(JobHistory.createPending(JobId.of("0"), created)))));

        jobId = jdbc.queryForObject("select ID from JOB where COMMIT_HASH = ?", Long.class, commitHash);
        Long pendingId = jdbc.queryForObject(
                "select ID from JOB_HISTORY where JOB_ID = ?", Long.class, jobId);
        LocalDateTime pendingCreatedAt = jdbc.queryForObject(
                "select CREATED_AT from JOB_HISTORY where ID = ?", LocalDateTime.class, pendingId);

        // The expected-previous history is read back from the database rather than reused from the
        // object we saved: isSameHistory compares the stored id and timestamp, and a client-side copy
        // would compare equal for the wrong reason.
        pending = JobHistory.reconstruct(
                JobHistoryId.of(String.valueOf(pendingId)), JobId.of(String.valueOf(jobId)),
                SequenceNumber.of(1), null, JobStatus.PENDING, ExecutionSystemActor.SYSTEM,
                pendingCreatedAt);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (jdbc != null && commitHash != null) {
            jdbc.update("delete from JOB_HISTORY where JOB_ID in (select ID from JOB where COMMIT_HASH = ?)",
                    commitHash);
            jdbc.update("delete from JOB where COMMIT_HASH = ?", commitHash);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void preservesCompareBeforeAppend() throws Exception {
        CyclicBarrier bothInside = new CyclicBarrier(2);

        Callable<Boolean> contender = () -> {
            // A separate adapter and transaction template per thread, so the two really are two
            // transactions. Sharing one would serialize them in the application and prove nothing.
            JobJpaRepositoryAdapter adapter = newAdapter();
            TransactionTemplate transactions = newTransactions();
            return transactions.execute(status -> {
                try {
                    bothInside.await(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                Optional<Long> appended = adapter.appendHistoryIfCurrent(nextTransition(), pending);
                return appended.isPresent();
            });
        };

        Future<Boolean> first = executor.submit(contender);
        Future<Boolean> second = executor.submit(contender);

        boolean firstWon = first.get(45, TimeUnit.SECONDS);
        boolean secondWon = second.get(45, TimeUnit.SECONDS);

        assertThat(List.of(firstWon, secondWon).stream().filter(won -> won).count())
                .as("exactly one contender may append; two winners means the job is dispatched twice, "
                        + "against the same commit, with two runners believing they own it")
                .isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from JOB_HISTORY where JOB_ID = ?", Integer.class, jobId))
                .as("one PENDING plus exactly one appended transition")
                .isEqualTo(2);
    }

    private Job nextTransition() {
        LocalDateTime created = pending.getCreatedAt();
        JobHistory inProgress = JobHistory.reconstruct(
                JobHistoryId.of("0"), JobId.of(String.valueOf(jobId)), SequenceNumber.of(2),
                RunnerId.of("7313"), JobStatus.IN_PROGRESS, ExecutionSystemActor.SYSTEM,
                created.plusSeconds(1));
        return Job.reconstruct(
                JobId.of(String.valueOf(jobId)), ExecutionRepositoryId.of(7311L),
                CommitHash.of(commitHash), BranchName.of("main"), ExecutionActorId.of(7312L),
                created, List.of(pending, inProgress));
    }

    private JobJpaRepositoryAdapter newAdapter() {
        return new JobJpaRepositoryAdapter(
                JpaMariaDbTestSupport.repository(emf, JobJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf, JobHistoryJpaRepository.class));
    }

    private TransactionTemplate newTransactions() {
        return new TransactionTemplate(new JpaTransactionManager(emf));
    }
}
