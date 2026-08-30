package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.application.contract.internal.DispatchableJob;
import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.domain.vo.JobStatus;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
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
 * Runs the translated dispatch query against real MariaDB.
 *
 * <p>The query's whole difficulty is the correlated {@code NOT EXISTS} that means "the newest history
 * row is PENDING". Every interesting case is a job that must NOT be returned:
 *
 * <ul>
 *   <li>a job whose newest history is IN_PROGRESS — already dispatched, must be skipped even though a
 *       PENDING row still exists behind it;</li>
 *   <li>a job in another organization, or another repository, when the runner is scoped;</li>
 *   <li>the tie-break when two history rows share a {@code CREATED_AT} and only the id separates
 *       them — a naive {@code MAX(CREATED_AT)} would call such a job dispatchable forever.</li>
 * </ul>
 *
 * <p>A test that only inserted one PENDING job and asserted it came back would pass against a query
 * with the {@code NOT EXISTS} deleted entirely.
 */
class JobDispatchJpaMariaDbIntegrationTest {

    private static final String PACKAGES = "io.jgitkins.server.execution.adapter.out.persistence.jpa";

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private JobDispatchJpaQueryAdapter adapter;
    private String marker;
    private Long organizationRepositoryId;
    private Long userRepositoryId;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(
                dataSource, "dispatch-query-jpa-integration", PACKAGES);
        EntityManagerFactory emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbc = new JdbcTemplate(dataSource);
        adapter = new JobDispatchJpaQueryAdapter(
                JpaMariaDbTestSupport.repository(emf, JobDispatchJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf, JobHistoryJpaRepository.class));

        marker = "jpa76" + Long.toString(System.nanoTime(), 36);
        organizationRepositoryId = insertRepository(marker + "-org", "ORGANIZATION", 7601L);
        userRepositoryId = insertRepository(marker + "-user", "USER", 7602L);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && marker != null) {
            jdbc.update("delete from JOB_HISTORY where JOB_ID in (select ID from JOB where COMMIT_HASH like ?)",
                    marker + "%");
            jdbc.update("delete from JOB where COMMIT_HASH like ?", marker + "%");
            jdbc.update("delete from REPOSITORY where PATH like ?", marker + "%");
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void loadsNextDispatchableJobAgainstMariaDb() {
        LocalDateTime base = LocalDateTime.now().withNano(0).minusHours(1);

        // Oldest, but already in progress: its newest history is not PENDING, so it must be skipped.
        Long dispatched = insertJob(organizationRepositoryId, marker + "-dispatched", base);
        insertHistory(dispatched, "PENDING", base);
        insertHistory(dispatched, "IN_PROGRESS", base.plusMinutes(1));

        // Next oldest and genuinely pending: this is the expected answer.
        Long pending = insertJob(organizationRepositoryId, marker + "-pending", base.plusMinutes(2));
        insertHistory(pending, "PENDING", base.plusMinutes(2));

        // Newer and pending: must lose the ORDER BY.
        Long newer = insertJob(organizationRepositoryId, marker + "-newer", base.plusMinutes(3));
        insertHistory(newer, "PENDING", base.plusMinutes(3));

        Optional<DispatchableJob> global = fetch(JobDispatchScope.GLOBAL, null);
        assertThat(global).isPresent();
        assertThat(global.get().jobId())
                .as("the already-dispatched job is older but its newest history is IN_PROGRESS; "
                        + "returning it would dispatch the same job twice")
                .isEqualTo(pending);
        assertThat(global.get().organizeId())
                .as("an organization-owned repository must carry its owner id through as the organize id")
                .isEqualTo(7601L);
        assertThat(global.get().repositoryClonePath()).isEqualTo("/" + marker + "-org.git");
        assertThat(global.get().job().getHistories())
                .as("the history list must come back with it, ordered, so the caller can compare "
                        + "identity before appending")
                .hasSize(1);
        assertThat(global.get().job().getHistories().get(0).getStatus()).isEqualTo(JobStatus.PENDING);

        assertThat(fetch(JobDispatchScope.ORGANIZE, 7601L))
                .isPresent()
                .get()
                .extracting(DispatchableJob::jobId)
                .isEqualTo(pending);

        assertThat(fetch(JobDispatchScope.ORGANIZE, 9_999_999L))
                .as("a runner scoped to another organization must get nothing, not the global answer")
                .isEmpty();

        assertThat(fetch(JobDispatchScope.REPOSITORY, organizationRepositoryId))
                .isPresent()
                .get()
                .extracting(DispatchableJob::jobId)
                .isEqualTo(pending);

        assertThat(fetch(JobDispatchScope.REPOSITORY, userRepositoryId))
                .as("that repository has no jobs; a scoped runner must not fall through to another one")
                .isEmpty();
    }

    @Test
    void skipsAJobWhoseNewestHistorySharesATimestampWithThePendingOne() {
        LocalDateTime moment = LocalDateTime.now().withNano(0);

        Long job = insertJob(userRepositoryId, marker + "-tie", moment);
        insertHistory(job, "PENDING", moment);
        insertHistory(job, "IN_PROGRESS", moment);

        assertThat(fetch(JobDispatchScope.REPOSITORY, userRepositoryId))
                .as("both history rows carry the same CREATED_AT, so only the id separates them; a "
                        + "newest-by-timestamp test would call this job dispatchable forever and every "
                        + "poll would re-dispatch it")
                .isEmpty();
    }

    @Test
    void reportsNoOrganizeIdForAUserOwnedRepository() {
        LocalDateTime moment = LocalDateTime.now().withNano(0);
        Long job = insertJob(userRepositoryId, marker + "-userowned", moment);
        insertHistory(job, "PENDING", moment);

        assertThat(fetch(JobDispatchScope.REPOSITORY, userRepositoryId))
                .isPresent()
                .get()
                .extracting(DispatchableJob::organizeId)
                .as("the owner id of a user-owned repository is a user id; passing it through as an "
                        + "organize id would name a different entity entirely")
                .isNull();
    }

    private Optional<DispatchableJob> fetch(JobDispatchScope scope, Long scopeTargetId) {
        return transactions.execute(status ->
                adapter.fetchNextJob(new RunnerDispatchContext(7603L, scope, scopeTargetId)));
    }

    private Long insertRepository(String path, String ownerType, Long ownerId) {
        jdbc.update("insert into REPOSITORY (NAME, PATH, OWNER_TYPE, OWNER_ID, CLONE_PATH, "
                        + "DEFAULT_BRANCH, VISIBILITY, STATUS) values (?,?,?,?,?,?,?,?)",
                path, path, ownerType, ownerId, "/" + path + ".git", "main", "PRIVATE", "REGISTERED");
        return jdbc.queryForObject("select ID from REPOSITORY where PATH = ?", Long.class, path);
    }

    private Long insertJob(Long repositoryId, String commitHash, LocalDateTime createdAt) {
        jdbc.update("insert into JOB (REPOSITORY_ID, COMMIT_HASH, BRANCH_NAME, TRIGGERED_BY, CREATED_AT) "
                + "values (?,?,?,?,?)", repositoryId, commitHash, "main", 7604L, createdAt);
        return jdbc.queryForObject("select ID from JOB where COMMIT_HASH = ?", Long.class, commitHash);
    }

    private void insertHistory(Long jobId, String status, LocalDateTime createdAt) {
        jdbc.update("insert into JOB_HISTORY (JOB_ID, STATUS, CREATED_AT) values (?,?,?)",
                jobId, status, createdAt);
    }
}
