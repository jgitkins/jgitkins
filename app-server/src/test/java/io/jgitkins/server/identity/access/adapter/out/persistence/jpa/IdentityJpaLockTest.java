package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
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
 * {@code findByIdForUpdate} must still take a row lock under JPA.
 *
 * <p>The MyBatis adapter reaches {@code selectByPrimaryKeyForUpdate}, whose mapper XML carries
 * {@code for update}. The JPA repository reaches {@code @Lock(PESSIMISTIC_WRITE)}. Neither is visible
 * to the type system, so a JPA adapter that reads without locking compiles, returns the same
 * {@code Optional<User>}, and passes every unit test - while dropping the serialization that
 * concurrent account activation depends on.
 *
 * <p>Asserts the observable consequence: one transaction holds the lock, a second times how long its
 * own lock call blocks. A locked read waits for the holder to commit; an unlocked read returns at
 * once. Real MariaDB because the behaviour is InnoDB's.
 */
class IdentityJpaLockTest {

    private static final Duration HOLD = Duration.ofMillis(1500);
    private static final Duration MIN_EXPECTED_WAIT = Duration.ofMillis(1000);

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private UserJpaPersistenceAdapter userAdapter;
    private Long userId;
    private String username;

    @BeforeEach
    void setUp() {
        assumeTrue(IdentityJpaTestSupport.mariaDbReachable(),
                "MariaDB is not reachable at " + IdentityJpaTestSupport.URL
                        + " -- the identity row-lock contract is UNVERIFIED, not satisfied.");

        DriverManagerDataSource dataSource = IdentityJpaTestSupport.dataSource();
        factoryBean = IdentityJpaTestSupport.entityManagerFactory(dataSource, "identity-lock");
        EntityManagerFactory emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        userAdapter = new UserJpaPersistenceAdapter(
                IdentityJpaTestSupport.repository(emf, UserJpaRepository.class));

        jdbc = new JdbcTemplate(dataSource);
        username = "jpa-identity-lock-" + System.nanoTime();
        jdbc.update("insert into USER (USERNAME, AUTHORITY, STATUS) values (?, 'USER', 'ACTIVE')", username);
        userId = jdbc.queryForObject("select ID from USER where USERNAME = ?", Long.class, username);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && username != null) {
            jdbc.update("delete from USER where USERNAME = ?", username);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void preservesFindByIdForUpdate() throws Exception {
        CountDownLatch holderHasLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> transactions.executeWithoutResult(status -> {
                assertThat(userAdapter.findByIdForUpdate(userId))
                        .as("the locked read must return the row, not only take the lock")
                        .isPresent();
                holderHasLock.countDown();
                sleep(HOLD);
            }));

            assertThat(holderHasLock.await(20, TimeUnit.SECONDS))
                    .as("the first transaction must acquire the lock before the second one tries")
                    .isTrue();

            Future<Long> contender = executor.submit(() -> transactions.execute(status -> {
                long startedAt = System.nanoTime();
                userAdapter.findByIdForUpdate(userId);
                return System.nanoTime() - startedAt;
            }));

            holder.get(30, TimeUnit.SECONDS);
            Duration waited = Duration.ofNanos(contender.get(30, TimeUnit.SECONDS));

            assertThat(waited)
                    .as("a second locked read on the same user must block until the holder commits; "
                            + "returning immediately means PESSIMISTIC_WRITE is not in effect and "
                            + "concurrent activation is no longer serialized")
                    .isGreaterThanOrEqualTo(MIN_EXPECTED_WAIT);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
