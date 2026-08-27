package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
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
 * A repository, its first member and its default branch must commit as one unit under JPA.
 *
 * <p>A repository row with no member is unreachable: every member-scoped operation, including adding
 * the first member, checks membership first. A repository with no default branch row is worse than
 * unreachable — clients see it and every clone fails. Neither state can be repaired by a later
 * validation, which is why this belongs in a transaction test rather than a service test.
 *
 * <p>It is proven again for JPA rather than inherited from the MyBatis equivalent because Hibernate
 * defers and reorders writes at flush time. "All three writes happened" and "all three writes are in
 * one transaction" are distinguishable statements under JPA, and only the second one holds the
 * invariant. A row-counting test that ran after a successful commit would pass either way.
 *
 * <p>Runs against real MariaDB: rollback is InnoDB's behaviour, not the provider's. When the database
 * is down the test skips, which records the atomicity as unverified rather than satisfied.
 */
class RepositoryJpaTransactionTest {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private RepositoryJpaPersistenceAdapter repositoryAdapter;
    private RepositoryMemberJpaPersistenceAdapter memberAdapter;
    private BranchJpaRepositoryAdapter branchAdapter;
    private String marker;

    @BeforeEach
    void setUp() {
        assumeTrue(JpaMariaDbTestSupport.mariaDbReachable(),
                "MariaDB is not reachable at " + JpaMariaDbTestSupport.URL
                        + " -- repository cutover atomicity is UNVERIFIED, not satisfied.");

        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "repository-jpa-transaction",
                "io.jgitkins.server.repository.adapter.out.persistence.jpa",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa",
                "io.jgitkins.server.identity.access.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();

        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        repositoryAdapter = new RepositoryJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, RepositoryJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf,
                        io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf,
                        io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaRepository.class),
                JpaMariaDbTestSupport.repository(emf,
                        io.jgitkins.server.collaboration.adapter.out.persistence.jpa
                                .OrganizeMemberJpaRepository.class),
                new CloneUrlBuilder(new RepositoryEndpointPort() {
                    @Override
                    public String restScheme() {
                        return "https";
                    }

                    @Override
                    public String serviceHost() {
                        return "example.invalid";
                    }
                }));
        memberAdapter = new RepositoryMemberJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, RepositoryMemberJpaRepository.class));
        branchAdapter = new BranchJpaRepositoryAdapter(
                JpaMariaDbTestSupport.repository(emf, BranchJpaRepository.class));

        jdbc = new JdbcTemplate(dataSource);
        marker = "jpa-tx-repo-" + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && marker != null) {
            jdbc.update("delete from BRANCH where REPOSITORY_ID in "
                    + "(select ID from REPOSITORY where PATH like ?)", marker + "%");
            jdbc.update("delete from REPOSITORY_MEMBER where REPOSITORY_ID in "
                    + "(select ID from REPOSITORY where PATH like ?)", marker + "%");
            jdbc.update("delete from REPOSITORY where PATH like ?", marker + "%");
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void preservesAtomicCutover() {
        String committedPath = marker + "-committed";

        Long committedId = transactions.execute(status -> {
            Long id = insertRepository(committedPath);
            memberAdapter.save(RepositoryMember.create(RepositoryId.of(id),
                    RepositoryMemberUserId.of(7201L), RepositoryMemberRole.MAINTAINER, LocalDateTime.now()));
            branchAdapter.save(Branch.create(id, committedPath + "-main", false, false, true));
            return id;
        });

        assertThat(countRepositories(committedPath)).as("the repository must be committed").isEqualTo(1);
        assertThat(countMembers(committedId))
                .as("its first member must commit with it; a repository with no member is unreachable "
                        + "because adding the first member checks membership")
                .isEqualTo(1);
        assertThat(countBranches(committedId))
                .as("its default branch must commit with it; clients see a repository with no branch "
                        + "and every clone fails")
                .isEqualTo(1);

        String rolledBackPath = marker + "-rolledback";
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            Long id = insertRepository(rolledBackPath);
            memberAdapter.save(RepositoryMember.create(RepositoryId.of(id),
                    RepositoryMemberUserId.of(7202L), RepositoryMemberRole.MAINTAINER, LocalDateTime.now()));
            branchAdapter.save(Branch.create(id, rolledBackPath + "-main", false, false, true));
            throw new IllegalStateException("post-write failure inside the cutover transaction");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countRepositories(rolledBackPath))
                .as("a failure after all three writes must take the repository back out")
                .isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from REPOSITORY_MEMBER where USER_ID = ?", Integer.class, 7202L))
                .as("and the member write with it")
                .isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from BRANCH where NAME = ?", Integer.class, rolledBackPath + "-main"))
                .as("and the branch write with it -- Hibernate flush ordering must not leave one behind")
                .isZero();
    }

    private Long insertRepository(String path) {
        RepositoryJpaEntity entity = new RepositoryJpaEntity();
        entity.setName(path);
        entity.setPath(path);
        entity.setOwnerType("USER");
        entity.setOwnerId(7201L);
        entity.setClonePath("/" + path + ".git");
        entity.setDefaultBranch("main");
        entity.setVisibility("PRIVATE");
        entity.setStatus("REGISTERED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return JpaMariaDbTestSupport
                .repository(factoryBean.getObject(), RepositoryJpaRepository.class)
                .save(entity)
                .getId();
    }

    private int countRepositories(String exactPath) {
        return jdbc.queryForObject("select count(*) from REPOSITORY where PATH = ?", Integer.class, exactPath);
    }

    private int countMembers(Long repositoryId) {
        return jdbc.queryForObject(
                "select count(*) from REPOSITORY_MEMBER where REPOSITORY_ID = ?", Integer.class, repositoryId);
    }

    private int countBranches(Long repositoryId) {
        return jdbc.queryForObject(
                "select count(*) from BRANCH where REPOSITORY_ID = ?", Integer.class, repositoryId);
    }
}
