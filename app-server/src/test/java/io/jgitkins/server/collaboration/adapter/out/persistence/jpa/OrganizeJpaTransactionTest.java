package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
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
 * Owner-bootstrap atomicity under JPA.
 *
 * <p>{@code OrganizeCreationMembershipBootstrapTest} proves for MyBatis that creating an
 * organization and its creator-owner membership commit together, and that a later failure rolls both
 * back. That guarantee is what stops an organization existing with no owner, which no later
 * validation can repair because every owner-only operation would be unreachable.
 *
 * <p>It has to be proven again for JPA rather than inherited. The two providers manage flushing
 * differently: Hibernate defers writes to flush time and can reorder them, so "both writes happened"
 * and "both writes are in the same transaction" are not the same statement under JPA the way they
 * effectively are under MyBatis. A provider swap that silently split the two writes across
 * transactions would leave the owner invariant intact in tests that only count rows after a
 * successful commit.
 *
 * <p>Runs against real MariaDB because rollback is InnoDB's behaviour, not the persistence
 * provider's. Skips when the database is down, in which case atomicity is unverified rather than
 * satisfied.
 */
class OrganizeJpaTransactionTest {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private OrganizeJpaPersistenceAdapter organizeAdapter;
    private OrganizeMemberJpaPersistenceAdapter memberAdapter;
    private String path;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "organize-jpa-transaction", "io.jgitkins.server.collaboration.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();

        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        organizeAdapter = new OrganizeJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, OrganizeJpaRepository.class));
        memberAdapter = new OrganizeMemberJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, OrganizeMemberJpaRepository.class));

        jdbc = new JdbcTemplate(dataSource);
        path = "jpa-tx-" + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && path != null) {
            jdbc.update("delete from ORGANIZE_MEMBER where ORGANIZE_ID in "
                    + "(select ID from ORGANIZE where PATH like ?)", path + "%");
            jdbc.update("delete from ORGANIZE where PATH like ?", path + "%");
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void preservesOwnerBootstrapAndMembershipAtomicity() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String committedPath = path + "-committed";

        Long committedId = transactions.execute(status -> {
            Organize organize = organizeAdapter.save(Organize.createWithoutEvent(
                    null, OrganizeName.from(committedPath), OrganizeOwnerId.of(4246L), "bootstrap", now));
            memberAdapter.save(OrganizeMember.create(
                    organize.getId(), MemberUserId.of(4246L), OrganizeMemberRole.OWNER, now));
            return organize.getId().getValue();
        });

        assertThat(countOrganizes(committedPath))
                .as("the organization must be committed")
                .isEqualTo(1);
        assertThat(countMembers(committedId))
                .as("the creator-owner membership must commit with it; an organization with no owner "
                        + "cannot be repaired later because every owner-only operation is unreachable")
                .isEqualTo(1);

        String rolledBackPath = path + "-rolledback";
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            Organize organize = organizeAdapter.save(Organize.createWithoutEvent(
                    null, OrganizeName.from(rolledBackPath), OrganizeOwnerId.of(4247L), "bootstrap", now));
            memberAdapter.save(OrganizeMember.create(
                    organize.getId(), MemberUserId.of(4247L), OrganizeMemberRole.OWNER, now));
            throw new IllegalStateException("required membership write failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countOrganizes(rolledBackPath))
                .as("a failure after the organization write must roll the organization back too, not "
                        + "leave it behind with no owner")
                .isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from ORGANIZE_MEMBER where USER_ID = ?", Integer.class, 4247L))
                .as("the membership write must roll back with it")
                .isZero();
    }

    private int countOrganizes(String exactPath) {
        return jdbc.queryForObject("select count(*) from ORGANIZE where PATH = ?", Integer.class, exactPath);
    }

    private int countMembers(Long organizeId) {
        return jdbc.queryForObject(
                "select count(*) from ORGANIZE_MEMBER where ORGANIZE_ID = ?", Integer.class, organizeId);
    }
}
