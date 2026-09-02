package io.jgitkins.server.collaboration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.translator.OrganizeEntityMbgMapper;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves that {@link OrganizeRepository#lockByIdForMembershipMutation} takes a real row lock.
 *
 * <p>Why this exists separately from {@code OrganizeMembershipConcurrencyIntegrationTest}: that test
 * runs on embedded H2 and orchestrates thread ordering with latches, so it verifies the membership
 * invariant given a lock, not that a lock is actually taken. The port only promises the lock in its
 * method name; the {@code for update} clause lives in {@code OrganizeEntityMbgMapper.xml:124} and
 * nowhere in the type system. A JPA adapter added later can implement the same signature with a
 * plain unlocked read, compile, satisfy every existing test, and silently lose serialization on
 * concurrent membership mutations.
 *
 * <p>This test closes that hole by asserting the observable consequence instead:
 *
 * <pre>
 *   T1: BEGIN --> lockByIdForMembershipMutation(id) --> hold HOLD --> COMMIT
 *   T2:              BEGIN --> lockByIdForMembershipMutation(id) ...........blocked....... returns
 *                             |                                                          |
 *                             +------------------- measured wait --------------------------+
 *
 *   locked read   : measured wait ~= HOLD          (T2 waits for T1 to commit)
 *   unlocked read : measured wait ~= 0             (T2 returns immediately)  --> FAILS
 * </pre>
 *
 * <p>It runs against real MariaDB because the lock is an InnoDB behaviour. H2 in MariaDB mode may
 * accept the same SQL without reproducing the same blocking, which would make a green result
 * meaningless. Task 2.103 moved that database into a Testcontainers singleton owned by
 * {@link JpaMariaDbTestSupport}, so the test no longer skips when nothing is listening on 53306 —
 * an unverified lock contract now reads as a failure, which is what it always was.
 */
class OrganizeLockContractMariaDbTest {

    /** Long enough that scheduling noise cannot explain the wait, short enough to keep the test fast. */
    private static final Duration HOLD = Duration.ofMillis(1500);

    /** T2 must wait for most of the hold. Slack absorbs connection setup and thread scheduling. */
    private static final Duration MIN_EXPECTED_WAIT = Duration.ofMillis(1000);

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private OrganizeRepository repository;
    private Long organizeId;
    private String path;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = JpaMariaDbTestSupport.dataSource();
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        SqlSessionFactory factory = sqlSessionFactory(dataSource);
        repository = new OrganizePersistenceAdapter(
                new SqlSessionTemplate(factory).getMapper(OrganizeEntityMbgMapper.class),
                org.mapstruct.factory.Mappers.getMapper(OrganizeDomainMapper.class));

        // A dedicated row, so the test never contends with anything else in a shared database.
        path = "lock-contract-" + System.nanoTime();
        jdbc.update("insert into ORGANIZE (NAME, PATH, OWNER_ID) values (?, ?, ?)", path, path, 4242L);
        organizeId = jdbc.queryForObject("select ID from ORGANIZE where PATH = ?", Long.class, path);
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && path != null) {
            jdbc.update("delete from ORGANIZE where PATH = ?", path);
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void lockByIdForMembershipMutationBlocksAConcurrentLockUntilTheHolderCommits() throws Exception {
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
                    .as("a second lock on the same row must block until the holder commits; returning "
                            + "immediately means the read was not locked, which is exactly how a JPA "
                            + "adapter would silently drop serialization")
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

    private SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new ClassPathResource("mapper/mbg/OrganizeEntityMbgMapper.xml"));
        factoryBean.setConfiguration(new org.apache.ibatis.session.Configuration());
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
