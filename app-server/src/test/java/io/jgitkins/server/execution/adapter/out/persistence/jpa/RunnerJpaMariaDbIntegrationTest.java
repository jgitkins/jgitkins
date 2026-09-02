package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

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

        // The row, not the return value. Carried over from RunnerScopeUpdateMariaDbTest, which was the
        // MyBatis copy of this class: the create branch returns the scope the caller passed in, so a
        // wrong row is invisible from the aggregate, and that is how it survived -- the branch mapped
        // the assignment from a database with no assignment row yet, which answers GLOBAL. Every runner
        // created through MyBatis was recorded as GLOBAL. Reading through findById below would catch it
        // too, but only because this adapter has no path that answers from the argument.
        assertThat(jdbc.queryForMap(
                "select TARGET_TYPE, TARGET_ID from RUNNER_ASSIGNMENT where RUNNER_ID = ?", runnerId))
                .as("a runner scoped to one repository must not be recorded as GLOBAL, which dispatches "
                        + "everything to it")
                .containsEntry("TARGET_TYPE", "REPOSITORY")
                .containsEntry("TARGET_ID", 7401L);

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
    void scopeUpdateTakesEffect() {
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
                .as("this assertion used to pin the defect at 900: the MyBatis adapter updated the "
                        + "assignment by a primary key its mapper never populates, so the statement "
                        + "resolved to `where ID = null` and scope changes had never taken effect. "
                        + "Both providers now append a row when the scope differs")
                .isEqualTo(901L);

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("one row for the create, one for the change")
                .isEqualTo(2);

        // Both writes land in the same second, so ASSIGNED_AT cannot order them -- the column is a
        // whole-second timestamp. Reading 901 above is what proves the id tiebreak is doing the work;
        // without it the winner is whichever row the engine returns first.
        assertThat(jdbc.queryForObject(
                "select count(distinct ASSIGNED_AT) from RUNNER_ASSIGNMENT where RUNNER_ID = ?",
                Integer.class, runnerId))
                .as("if these ever land in different seconds the tiebreak stops being exercised here "
                        + "and this test quietly weakens")
                .isEqualTo(1);
    }

    @Test
    void narrowingToGlobalDropsTheStaleTargetAndStaysIdempotent() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token + "-to-global", "to global", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 930L, null, null, now)));
        Long runnerId = created.getId();

        // scopeTargetId is deliberately left at 930 while the type becomes GLOBAL. Every other scope
        // test in this class uses REPOSITORY with a matching target, so the normalization the whole
        // idempotency comparison rests on -- requiresTargetId() ? scopeTargetId : null -- had no test:
        // deleting the guard on either the write or the compare left the suite green.
        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token + "-to-global", "to global", RunnerStatus.ONLINE,
                RunnerScopeType.GLOBAL, 930L, null, null, now)));

        assertThat(jdbc.queryForMap(
                "select TARGET_TYPE, TARGET_ID from RUNNER_ASSIGNMENT where RUNNER_ID = ?"
                        + " order by ASSIGNED_AT desc, ID desc limit 1", runnerId))
                .as("a GLOBAL scope has no target, and writing the caller's stale value would let it "
                        + "leak into a scope that must ignore it")
                .containsEntry("TARGET_TYPE", "GLOBAL")
                .containsEntry("TARGET_ID", null);

        Runner reloaded = transactions.execute(status -> adapter.findById(runnerId).orElseThrow());
        assertThat(reloaded.getScopeType()).isEqualTo(RunnerScopeType.GLOBAL);
        assertThat(reloaded.getScopeTargetId()).isNull();

        // The idempotency half: the comparison must normalize the same way the write does, or a GLOBAL
        // runner carrying a stale target appends on every restart -- the restart log both adapters'
        // javadoc claims to prevent.
        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token + "-to-global", "to global", RunnerStatus.ONLINE,
                RunnerScopeType.GLOBAL, 930L, null, null, now)));

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("one row for the create, one for the narrowing, and nothing for the re-save")
                .isEqualTo(2);
    }

    @Test
    void reSavingTheSameScopeWritesNoRow() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Runner created = transactions.execute(status -> adapter.save(Runner.restore(
                null, token + "-idempotent", "same scope twice", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 910L, null, null, now)));
        Long runnerId = created.getId();

        // What activate does on every runner restart: save the aggregate back with its scope
        // unchanged. Appending unconditionally would turn RUNNER_ASSIGNMENT into a restart log.
        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token + "-idempotent", "same scope twice", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 910L, null, null, now)));
        transactions.executeWithoutResult(status -> adapter.save(Runner.restore(
                runnerId, token + "-idempotent", "same scope twice", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 910L, null, null, now)));

        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId))
                .as("three saves, one scope: the two that changed nothing must write nothing")
                .isEqualTo(1);
    }
}
