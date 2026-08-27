package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
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
 * A runner and its assignment row must commit together, and roll back together.
 *
 * <p>A runner row with no assignment row is not a broken record — it reads as {@code GLOBAL}, which is
 * the fallback for runners that predate assignments. That is precisely why this needs a test: the
 * failure is silent. A half-committed registration would produce a runner that accepts jobs from
 * *every* repository instead of the one it was scoped to, and nothing would look wrong.
 *
 * <p>Runs against real MariaDB because rollback is InnoDB's behaviour. Skipping means the atomicity is
 * unverified, not satisfied.
 */
class RunnerJpaTransactionTest {

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
                "MariaDB is not reachable at " + JpaMariaDbTestSupport.URL
                        + " -- runner registration atomicity is UNVERIFIED, not satisfied.");

        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "runner-jpa-transaction", PACKAGES);
        emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbc = new JdbcTemplate(dataSource);
        adapter = new RunnerJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, RunnerJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf, RunnerAssignmentJpaRepository.class));
        token = "jpa74tx" + Long.toString(System.nanoTime(), 36);
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
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void persistsRunnerAndAssignmentsAtomically() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String committed = token + "-committed";

        Long committedId = transactions.execute(status -> adapter.save(Runner.restore(
                null, committed, "atomic", RunnerStatus.ONLINE,
                RunnerScopeType.REPOSITORY, 7411L, null, null, now)).getId());

        assertThat(countRunners(committed)).as("the runner must be committed").isEqualTo(1);
        assertThat(countAssignments(committedId))
                .as("its assignment must commit with it; a runner with no assignment row reads as "
                        + "GLOBAL and would accept jobs from every repository")
                .isEqualTo(1);

        String rolledBack = token + "-rolledback";
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            adapter.save(Runner.restore(null, rolledBack, "atomic", RunnerStatus.ONLINE,
                    RunnerScopeType.REPOSITORY, 7412L, null, null, now));
            throw new IllegalStateException("failure after both writes inside the registration");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countRunners(rolledBack))
                .as("a failure after both writes must take the runner back out")
                .isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where TARGET_ID = ?", Integer.class, 7412L))
                .as("and the assignment with it -- an orphan assignment would later attach to whichever "
                        + "runner reuses that auto-increment id")
                .isZero();
    }

    private int countRunners(String exactToken) {
        return jdbc.queryForObject("select count(*) from RUNNER where TOKEN = ?", Integer.class, exactToken);
    }

    private int countAssignments(Long runnerId) {
        return jdbc.queryForObject(
                "select count(*) from RUNNER_ASSIGNMENT where RUNNER_ID = ?", Integer.class, runnerId);
    }
}
