package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises {@code RunnerJpaPersistenceAdapter} against the real MariaDB schema.
 */
class RunnerJpaMariaDbIntegrationTest {

    private static final String PACKAGES = "io.jgitkins.server.execution.adapter.out.persistence.jpa";

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private EntityManagerFactory emf;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private RunnerJpaPersistenceAdapter adapter;
    private String token;

    @BeforeEach
    void setUp() {
        assumeTrue(JpaMariaDbTestSupport.mariaDbReachable(),
                "MariaDB on 127.0.0.1:53306 is required for execution-runner JPA evidence");

        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "runner-jpa-integration", PACKAGES);
        emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbc = new JdbcTemplate(dataSource);
        adapter = new RunnerJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, RunnerJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf, RunnerAssignmentJpaRepository.class));
        token = "jpa74" + Long.toString(System.nanoTime(), 36);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && token != null) {
            jdbc.update("delete from RUNNER_ASSIGNMENT where RUNNER_ID in "
                    + "(select ID from RUNNER where TOKEN like ?)", token + "%");
            jdbc.update("delete from RUNNER where TOKEN like ?", token + "%");
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void persistsRunnerAssignmentsAgainstMariaDb() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner saved = transactions.execute(status -> adapter.save(Runner.restore(
                null, token, "jpa evidence", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 7401L, "10.0.0.1", null, now)));

        assertThat(saved.getId()).as("the generated id must come back to the caller").isNotNull();
        Long runnerId = saved.getId();

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("creating a runner must create its assignment row in the same call")
                .isEqualTo(1);

        Runner byId = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(byId.getToken()).isEqualTo(token);
        assertThat(byId.getScopeType()).isEqualTo(RunnerScopeType.REPOSITORY);
        assertThat(byId.getScopeTargetId()).isEqualTo(7401L);
        assertThat(byId.getIpAddress()).isEqualTo("10.0.0.1");

        Runner byToken = transactions.execute(status -> adapter.findByToken(token).orElseThrow());
        assertThat(byToken.getId()).isEqualTo(runnerId);

        java.util.Optional<Runner> blank = transactions.execute(status -> adapter.findByToken("   "));
        assertThat(blank)
                .as("a blank token must not reach the database")
                .isEmpty();

        transactions.executeWithoutResult(status -> adapter.deleteById(runnerId));
        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("deleting a runner must take its assignment rows with it, or the next runner to "
                        + "reuse that id inherits a stale dispatch scope")
                .isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER where ID = ?", Integer.class, runnerId)).isZero();
    }

    @Test
    void treatsARunnerWithNoAssignmentRowAsGlobal() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        jdbc.update("insert into RUNNER (TOKEN, DESCRIPTION, STATUS, CREATED_AT) values (?,?,?,?)",
                token + "-bare", "no assignment row", "ONLINE", now);
        Long runnerId = jdbc.queryForObject(
                "select ID from RUNNER where TOKEN = ?", Long.class, token + "-bare");

        Runner loaded = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(loaded.getScopeType())
                .as("a runner predating assignments must still dispatch; GLOBAL is the fallback, not "
                        + "an error")
                .isEqualTo(RunnerScopeType.GLOBAL);
        assertThat(loaded.getScopeTargetId()).isNull();
    }

    @Test
    void readsTheNewestAssignmentWhenARunnerHasSeveral() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        jdbc.update("insert into RUNNER (TOKEN, STATUS, CREATED_AT) values (?,?,?)",
                token + "-multi", "ONLINE", now);
        Long runnerId = jdbc.queryForObject(
                "select ID from RUNNER where TOKEN = ?", Long.class, token + "-multi");

        jdbc.update("insert into RUNNER_ASSIGNMENT (RUNNER_ID, TARGET_TYPE, TARGET_ID, ASSIGNED_AT) "
                + "values (?,?,?,?)", runnerId, "REPOSITORY", 111L, now.minusHours(2));
        jdbc.update("insert into RUNNER_ASSIGNMENT (RUNNER_ID, TARGET_TYPE, TARGET_ID, ASSIGNED_AT) "
                + "values (?,?,?,?)", runnerId, "REPOSITORY", 222L, now);

        Runner loaded = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(loaded.getScopeTargetId())
                .as("RUNNER_ASSIGNMENT has no unique key on RUNNER_ID, so rows accumulate and the "
                        + "effective scope is the newest by ASSIGNED_AT")
                .isEqualTo(222L);
    }

    @Test
    void scopeUpdateIsANoOpUnderBothProviders() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token + "-scope", "scope defect", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 900L, null, null, now)));
        Long runnerId = created.getId();

        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token + "-scope", "scope defect", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 901L, null, null, now)));

        Runner reloaded = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(reloaded.getScopeTargetId())
                .as("KNOWN DEFECT, pinned rather than endorsed: the MyBatis adapter updates the "
                        + "assignment by a primary key its mapper never populates, so the statement "
                        + "resolves to `where ID = null` and changes nothing. Scope updates have never "
                        + "taken effect. This adapter reproduces that so flipping the selector stays "
                        + "invisible; fixing it belongs in its own task, against both providers at once. "
                        + "When that task lands, this assertion is the one that must change.")
                .isEqualTo(900L);

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("and no second assignment row is written either -- the update branch writes no "
                        + "assignment at all")
                .isEqualTo(1);
    }
}
