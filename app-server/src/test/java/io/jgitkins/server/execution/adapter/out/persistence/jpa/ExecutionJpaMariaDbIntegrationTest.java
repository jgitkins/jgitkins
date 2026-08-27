package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises {@code JobJpaRepositoryAdapter} against the real MariaDB schema.
 *
 * <p>Two things are worth proving here beyond "the insert works". First, the positional sequence
 * number: {@code JOB_HISTORY} has no sequence column, so the value comes from the row's place in the
 * ordered history. A round trip through two histories is the only way to see that the ordering is
 * {@code CREATED_AT ASC, ID ASC} and not insertion-order-by-luck.
 *
 * <p>Second, that {@code appendHistoryIfCurrent} actually refuses. A test that only exercised the
 * accepting path would pass against an adapter that had dropped the comparison entirely.
 */
class ExecutionJpaMariaDbIntegrationTest {

    private static final String PACKAGES = "io.jgitkins.server.execution.adapter.out.persistence.jpa";

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private EntityManagerFactory emf;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private JobJpaRepositoryAdapter adapter;
    private String commitHash;

    @BeforeEach
    void setUp() {
        assumeTrue(JpaMariaDbTestSupport.mariaDbReachable(),
                "MariaDB on 127.0.0.1:53306 is required for execution-job JPA evidence");

        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(
                dataSource, "execution-jpa-integration", PACKAGES);
        emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbc = new JdbcTemplate(dataSource);

        adapter = new JobJpaRepositoryAdapter(
                JpaMariaDbTestSupport.repository(emf, JobJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf, JobHistoryJpaRepository.class));

        // COMMIT_HASH is varchar(64); a nanoTime in base 36 stays well inside it and keeps parallel
        // runs of this test from colliding on the cleanup predicate.
        commitHash = "jpa73" + Long.toString(System.nanoTime(), 36);
    }

    @AfterEach
    void tearDown() {
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
    void persistsJobHistoryAgainstMariaDb() {
        LocalDateTime created = LocalDateTime.now().withNano(0);

        transactions.executeWithoutResult(status -> adapter.save(Job.reconstruct(
                JobId.of("0"),
                ExecutionRepositoryId.of(7301L),
                CommitHash.of(commitHash),
                BranchName.of("main"),
                ExecutionActorId.of(7302L),
                created,
                List.of(JobHistory.createPending(JobId.of("0"), created)))));

        Long jobId = jdbc.queryForObject("select ID from JOB where COMMIT_HASH = ?", Long.class, commitHash);
        assertThat(jobId).as("the job must be inserted with a generated id").isNotNull();
        assertThat(jdbc.queryForObject(
                "select count(*) from JOB_HISTORY where JOB_ID = ?", Integer.class, jobId))
                .as("its initial history must be inserted with it")
                .isEqualTo(1);

        Job loaded = transactions.execute(status -> adapter.findById(jobId).orElseThrow());
        assertThat(loaded.getCommitHash().getValue()).isEqualTo(commitHash);
        assertThat(loaded.getHistories()).hasSize(1);
        JobHistory pending = loaded.getHistories().get(0);
        assertThat(pending.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(pending.getSeqNo())
                .as("the sequence number is positional; there is no column for it")
                .isEqualTo(SequenceNumber.of(1));

        // Appending with the correct expected-previous history must succeed.
        JobHistory inProgress = JobHistory.reconstruct(
                JobHistoryId.of("0"), JobId.of(String.valueOf(jobId)), SequenceNumber.of(2),
                RunnerId.of("7303"), JobStatus.IN_PROGRESS, ExecutionSystemActor.SYSTEM,
                created.plusSeconds(1));
        Job withNext = Job.reconstruct(
                JobId.of(String.valueOf(jobId)), ExecutionRepositoryId.of(7301L),
                CommitHash.of(commitHash), BranchName.of("main"), ExecutionActorId.of(7302L),
                created, List.of(pending, inProgress));

        Optional<Long> appended = transactions.execute(status ->
                adapter.appendHistoryIfCurrent(withNext, pending));
        assertThat(appended).as("the expected-previous history matched, so the append must happen").isPresent();

        // And appending again against the now-stale expected-previous history must refuse.
        Optional<Long> rejected = transactions.execute(status ->
                adapter.appendHistoryIfCurrent(withNext, pending));
        assertThat(rejected)
                .as("the latest history is no longer the one the caller based its decision on; "
                        + "appending anyway is the double-dispatch this method exists to prevent")
                .isEmpty();

        assertThat(jdbc.queryForObject(
                "select count(*) from JOB_HISTORY where JOB_ID = ?", Integer.class, jobId))
                .as("exactly one append happened, not two")
                .isEqualTo(2);

        Job reloaded = transactions.execute(status -> adapter.findById(jobId).orElseThrow());
        assertThat(reloaded.getHistories())
                .extracting(history -> history.getSeqNo().getValue())
                .as("ordering is CREATED_AT ASC, ID ASC, so the sequence renumbers from the query order")
                .containsExactly(1, 2);
        assertThat(reloaded.getHistories().get(1).getRunnerId().getValue()).isEqualTo("7303");
    }
}
