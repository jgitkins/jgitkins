package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.service.OrganizeMemberService;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Two owners removing themselves at once must leave one behind.
 *
 * <p>Was {@code OrganizeMembershipConcurrencyIntegrationTest}, over the MyBatis adapters on an
 * embedded H2. Two things changed in the move and only one of them was forced.
 *
 * <p>The forced one is the provider: app-server runs JPA for this slice now, so the invariant has to
 * be proven where it actually runs. {@code OrganizeJpaLockContractMariaDbTest} covers the lock
 * itself -- that a second lock call blocks until the holder commits -- and that is a different claim
 * from this one. A correct lock with a wrong last-owner check still ends with zero owners, and an
 * organization with no owner cannot be repaired: every owner-only operation, including adding an
 * owner, is unreachable.
 *
 * <p>The unforced one is the database. The original ran on H2, whose {@code for update} is not
 * InnoDB's, so it could report success on serialization the real database would not give. Since task
 * 2.103 there is a Testcontainers MariaDB singleton behind {@link JpaMariaDbTestSupport}, and using
 * it makes this the first time the outcome is asserted against the locking the production database
 * actually performs.
 *
 * <p>The interleaving is still forced rather than hoped for: the spy holds the first lock open until
 * the second call has been observed, so "one wins and one is denied" is the assertion rather than
 * whatever the scheduler happened to do.
 */
class OrganizeMembershipConcurrencyMariaDbTest {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private OrganizeMemberService service;
    private Long organizeId;
    private String path;

    private CountDownLatch firstLockAcquired;
    private CountDownLatch secondLockAttempted;
    private CountDownLatch releaseFirstLock;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource,
                "organize-membership-concurrency",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();

        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        path = "race-" + Long.toString(System.nanoTime(), 36);

        OrganizeJpaPersistenceAdapter organizeAdapter = new OrganizeJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, OrganizeJpaRepository.class));
        OrganizeMemberJpaPersistenceAdapter memberAdapter = new OrganizeMemberJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, OrganizeMemberJpaRepository.class));

        firstLockAcquired = new CountDownLatch(1);
        secondLockAttempted = new CountDownLatch(1);
        releaseFirstLock = new CountDownLatch(1);

        AtomicInteger lockCalls = new AtomicInteger();
        OrganizeRepository lockedRepository = Mockito.spy((OrganizeRepository) organizeAdapter);
        Mockito.doAnswer(invocation -> {
            int call = lockCalls.incrementAndGet();
            if (call > 1) {
                secondLockAttempted.countDown();
            }
            var result = organizeAdapter.lockByIdForMembershipMutation(invocation.getArgument(0));
            if (call == 1) {
                firstLockAcquired.countDown();
                if (!releaseFirstLock.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("first lock release timed out");
                }
            }
            return result;
        }).when(lockedRepository).lockByIdForMembershipMutation(Mockito.any());

        service = new OrganizeMemberService(memberAdapter, memberAdapter, lockedRepository);

        transactions.executeWithoutResult(status -> {
            Organize saved = organizeAdapter.save(Organize.createWithoutEvent(
                    null, OrganizeName.from(path), OrganizeOwnerId.of(7L), "description",
                    LocalDateTime.now().withNano(0)));
            organizeId = saved.getId().getValue();
            memberAdapter.save(OrganizeMember.create(
                    saved.getId(), MemberUserId.of(7L), OrganizeMemberRole.OWNER, null));
            memberAdapter.save(OrganizeMember.create(
                    saved.getId(), MemberUserId.of(8L), OrganizeMemberRole.OWNER, null));
        });
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && organizeId != null) {
            jdbc.update("delete from ORGANIZE_MEMBER where ORGANIZE_ID = ?", organizeId);
            jdbc.update("delete from ORGANIZE where ID = ?", organizeId);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void concurrentDistinctOwnerSelfRemovalLeavesExactlyOneOwner() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> results = new ArrayList<>();
            results.add(executor.submit(() -> attemptSelfRemove(start, 7L)));
            results.add(executor.submit(() -> attemptSelfRemove(start, 8L)));

            assertThat(firstLockAcquired.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(secondLockAttempted.await(30, TimeUnit.SECONDS)).isTrue();
            releaseFirstLock.countDown();

            List<String> outcomes = results.stream().map(future -> {
                try {
                    return future.get(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            assertThat(outcomes)
                    .as("both removals are individually legal; serialization is what makes the second "
                            + "one the last owner and therefore denied")
                    .containsExactlyInAnyOrder("success", "denied");
            assertThat(jdbc.queryForObject(
                    "select count(*) from ORGANIZE_MEMBER where ORGANIZE_ID = ? and ROLE = 'OWNER'",
                    Integer.class, organizeId))
                    .as("an organization with no owner is unrepairable: adding an owner is itself an "
                            + "owner-only operation")
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select USER_ID from ORGANIZE_MEMBER where ORGANIZE_ID = ? and ROLE = 'OWNER'",
                    Long.class, organizeId))
                    .as("which of the two survives is the scheduler's business, that one does is not")
                    .isIn(7L, 8L);
        } finally {
            releaseFirstLock.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    private String attemptSelfRemove(CyclicBarrier start, long userId) {
        try {
            start.await(30, TimeUnit.SECONDS);
            transactions.executeWithoutResult(status ->
                    service.removeOrganizeMember(organizeId, userId, userId));
            return "success";
        } catch (OrganizeAccessDeniedException e) {
            return "denied";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
