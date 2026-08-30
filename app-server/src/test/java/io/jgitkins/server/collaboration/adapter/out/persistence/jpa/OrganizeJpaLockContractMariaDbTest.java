package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The JPA half of the row-lock contract.
 *
 * <p>{@code OrganizeLockContractMariaDbTest} proves the MyBatis adapter blocks a concurrent lock.
 * This proves the JPA adapter does the same, and it is the reason {@code OrganizeJpaRepository}
 * declares {@code @Lock(PESSIMISTIC_WRITE)} instead of a plain {@code findById}.
 *
 * <p>Why the annotation is not enough on its own: the port promises the lock in a method name only.
 * A JPA adapter that reads without locking compiles, returns the same type, and passes every unit
 * test and every H2 profile, while silently dropping the serialization the owner invariant depends
 * on. The two implementations must be interchangeable under the selector, so both are held to the
 * same observable behaviour rather than the same signature.
 *
 * <p>Same shape as the MyBatis test: one transaction holds the lock, a second times how long its own
 * lock call blocks. A locked read waits for the holder to commit; an unlocked read returns at once.
 * Real MariaDB because the behaviour is InnoDB's — since task 2.103 that database is the
 * Testcontainers singleton owned by {@link JpaMariaDbTestSupport}, so there is no longer a state in
 * which this contract is unverified and the suite still reports success.
 */
class OrganizeJpaLockContractMariaDbTest {

    private static final Duration HOLD = Duration.ofMillis(1500);
    private static final Duration MIN_EXPECTED_WAIT = Duration.ofMillis(1000);

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private OrganizeRepository repository;
    private Long organizeId;
    private String path;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();

        factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("io.jgitkins.server.collaboration.adapter.out.persistence.jpa");
        factoryBean.setPersistenceUnitName("organize-lock-contract");
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // The schema is owned by app-server/data/ddl.sql. A test that creates its own tables would
        // not be testing the real ones.
        factoryBean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factoryBean.afterPropertiesSet();

        EntityManagerFactory emf = factoryBean.getObject();
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        // A shared EntityManager proxy, not emf.createEntityManager(). A plain EntityManager is not
        // transaction-aware and would be shared across both threads, so the repository would never
        // join the TransactionTemplate's transaction and the lock would never be taken. The proxy
        // resolves to the thread-bound transactional EntityManager, which is what Spring injects
        // into a repository in the real application.
        OrganizeJpaRepository jpaRepository = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(emf))
                .getRepository(OrganizeJpaRepository.class);
        repository = new OrganizeJpaPersistenceAdapter(jpaRepository);

        jdbc = new JdbcTemplate(dataSource);
        path = "jpa-lock-contract-" + System.nanoTime();
        jdbc.update("insert into ORGANIZE (NAME, PATH, OWNER_ID) values (?, ?, ?)", path, path, 4243L);
        organizeId = jdbc.queryForObject("select ID from ORGANIZE where PATH = ?", Long.class, path);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && path != null) {
            jdbc.update("delete from ORGANIZE where PATH = ?", path);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void jpaLockByIdForMembershipMutationBlocksAConcurrentLockUntilTheHolderCommits() throws Exception {
        CountDownLatch holderHasLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> transactions.executeWithoutResult(status -> {
                repository.lockByIdForMembershipMutation(OrganizeId.of(organizeId));
                holderHasLock.countDown();
                sleep(HOLD);
            }));

            assertThat(holderHasLock.await(20, TimeUnit.SECONDS))
                    .as("the first transaction must acquire the lock before the second one tries")
                    .isTrue();

            Future<Long> contender = executor.submit(() -> transactions.execute(status -> {
                long startedAt = System.nanoTime();
                repository.lockByIdForMembershipMutation(OrganizeId.of(organizeId));
                return System.nanoTime() - startedAt;
            }));

            holder.get(30, TimeUnit.SECONDS);
            Duration waited = Duration.ofNanos(contender.get(30, TimeUnit.SECONDS));

            assertThat(waited)
                    .as("the JPA adapter must block a second lock on the same row until the holder "
                            + "commits; returning immediately means @Lock(PESSIMISTIC_WRITE) is not in "
                            + "effect and the adapter is not interchangeable with the MyBatis one")
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
