package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
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
 * The reference slice exercised end to end against real MariaDB.
 *
 * <p>This is the evidence Tasks 2.70-2.77 inherit. The unit tests prove mapping and the lock tests
 * prove blocking, but neither writes a row through Hibernate into the actual schema. Generated
 * identity, the {@code NOT NULL UNIQUE} constraint on {@code PATH}, timestamp columns with database
 * defaults, and the not-found path are all places where H2 in MariaDB mode and InnoDB can differ, so
 * they are checked here rather than assumed.
 *
 * <p>Skips when the database is unreachable, in which case the reference slice is unverified rather
 * than satisfied.
 */
class OrganizeJpaMariaDbIntegrationTest {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private OrganizeJpaPersistenceAdapter organizeAdapter;
    private OrganizeMemberJpaPersistenceAdapter memberAdapter;
    private String path;

    @BeforeEach
    void setUp() {
        assumeTrue(OrganizeJpaTestSupport.mariaDbReachable(),
                "MariaDB is not reachable at " + OrganizeJpaTestSupport.URL
                        + " -- the reference slice is UNVERIFIED, not satisfied.");

        DriverManagerDataSource dataSource = OrganizeJpaTestSupport.dataSource();
        factoryBean = OrganizeJpaTestSupport.entityManagerFactory(dataSource, "organize-reference-slice");
        EntityManagerFactory emf = factoryBean.getObject();

        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        organizeAdapter = new OrganizeJpaPersistenceAdapter(
                OrganizeJpaTestSupport.repository(emf, OrganizeJpaRepository.class));
        memberAdapter = new OrganizeMemberJpaPersistenceAdapter(
                OrganizeJpaTestSupport.repository(emf, OrganizeMemberJpaRepository.class));

        jdbc = new JdbcTemplate(dataSource);
        path = "jpa-reference-" + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && path != null) {
            jdbc.update("delete from ORGANIZE_MEMBER where ORGANIZE_ID in "
                    + "(select ID from ORGANIZE where PATH = ?)", path);
            jdbc.update("delete from ORGANIZE where PATH = ?", path);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void persistsReferenceSliceAgainstMariaDb() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Long generatedId = transactions.execute(status -> {
            Organize saved = organizeAdapter.save(Organize.createWithoutEvent(
                    null, OrganizeName.from(path), OwnerId.of(4244L), "reference slice", now));
            assertThat(saved.getId())
                    .as("MariaDB assigns the identity; the adapter must return it rather than the null "
                            + "it was handed, or callers cannot address the row they just wrote")
                    .isNotNull();
            return saved.getId().getValue();
        });

        assertThat(jdbc.queryForObject("select PATH from ORGANIZE where ID = ?", String.class, generatedId))
                .as("PATH is derived from the name, matching what MyBatis writes")
                .isEqualTo(path);

        transactions.executeWithoutResult(status -> {
            assertThat(organizeAdapter.findById(OrganizeId.of(generatedId)))
                    .hasValueSatisfying(found -> {
                        assertThat(found.getName().getValue()).isEqualTo(path);
                        assertThat(found.getOwnerId().getValue()).isEqualTo(4244L);
                        assertThat(found.getDescription()).isEqualTo("reference slice");
                    });
            assertThat(organizeAdapter.findByName(OrganizeName.from(path))).isPresent();
        });

        transactions.executeWithoutResult(status -> {
            memberAdapter.save(OrganizeMember.create(
                    OrganizeId.of(generatedId), MemberUserId.of(4244L), OrganizeMemberRole.OWNER, now));
            memberAdapter.save(OrganizeMember.create(
                    OrganizeId.of(generatedId), MemberUserId.of(4245L), OrganizeMemberRole.MEMBER, now));
        });

        transactions.executeWithoutResult(status -> {
            assertThat(memberAdapter.findAllByOrganizeId(OrganizeId.of(generatedId))).hasSize(2);
            assertThat(memberAdapter.existsByOrganizeIdAndUserId(
                    OrganizeId.of(generatedId), MemberUserId.of(4245L))).isTrue();
            assertThat(memberAdapter.findRoleByOrganizeIdAndUserId(generatedId, 4244L))
                    .contains(OrganizeMemberRole.OWNER);
            assertThat(memberAdapter.countOwnersByOrganizeId(generatedId)).isEqualTo(1L);
        });

        transactions.executeWithoutResult(status ->
                memberAdapter.deleteByOrganizeIdAndUserId(OrganizeId.of(generatedId), MemberUserId.of(4245L)));
        transactions.executeWithoutResult(status ->
                assertThat(memberAdapter.findAllByOrganizeId(OrganizeId.of(generatedId))).hasSize(1));

        // A positive id that cannot exist: OrganizeId rejects non-positive values, so the not-found
        // path has to be reached with a valid identifier rather than a sentinel.
        long absentId = Long.MAX_VALUE;
        transactions.executeWithoutResult(status ->
                assertThatThrownBy(() -> organizeAdapter.lockByIdForMembershipMutation(OrganizeId.of(absentId)))
                        .as("a missing row must raise the same domain exception MyBatis raises, not an "
                                + "empty Optional or a provider-specific error")
                        .isInstanceOf(OrganizeNotFoundException.class));
    }
}
