package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaEntity;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaEntity;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaEntity;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaRepository;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exercises the repository-context JPA adapters against the real MariaDB schema.
 *
 * <p>The three cross-context reads are the reason this test needs users and organizations, not just
 * repositories: {@code loadRepositoryByPath} resolves a namespace to either a user or an
 * organization, and {@code loadVisibleRepositories} filters by the requester's organization
 * memberships. A test that only inserted repositories would exercise none of that and would still
 * pass, which is how a namespace-resolution regression reaches production.
 *
 * <p>The persistence unit spans three packages for the same reason.
 */
class RepositoryJpaMariaDbIntegrationTest {

    private static final String PACKAGES_REPOSITORY =
            "io.jgitkins.server.repository.adapter.out.persistence.jpa";
    private static final String PACKAGES_COLLABORATION =
            "io.jgitkins.server.collaboration.adapter.out.persistence.jpa";
    private static final String PACKAGES_IDENTITY =
            "io.jgitkins.server.identity.access.adapter.out.persistence.jpa";

    private EntityManagerFactory emf;
    private TransactionTemplate transactionTemplate;
    private JdbcTemplate jdbcTemplate;
    private RepositoryJpaPersistenceAdapter repositoryAdapter;
    private RepositoryMemberJpaPersistenceAdapter memberAdapter;
    private BranchJpaRepositoryAdapter branchWriteAdapter;
    private BranchJpaQueryAdapter branchQueryAdapter;
    private UserJpaRepository users;
    private OrganizeJpaRepository organizations;
    private OrganizeMemberJpaRepository organizationMembers;

    private String suffix;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        emf = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "repository-jpa-integration",
                PACKAGES_REPOSITORY, PACKAGES_COLLABORATION, PACKAGES_IDENTITY).getObject();
        transactionTemplate = new TransactionTemplate(new JpaTransactionManager(emf));
        jdbcTemplate = new JdbcTemplate(dataSource);

        RepositoryJpaRepository repositories = JpaMariaDbTestSupport.repository(emf, RepositoryJpaRepository.class);
        RepositoryMemberJpaRepository members =
                JpaMariaDbTestSupport.repository(emf, RepositoryMemberJpaRepository.class);
        BranchJpaRepository branches = JpaMariaDbTestSupport.repository(emf, BranchJpaRepository.class);
        users = JpaMariaDbTestSupport.repository(emf, UserJpaRepository.class);
        organizations = JpaMariaDbTestSupport.repository(emf, OrganizeJpaRepository.class);
        organizationMembers = JpaMariaDbTestSupport.repository(emf, OrganizeMemberJpaRepository.class);

        // The real CloneUrlBuilder over a stubbed endpoint port: the builder's own normalization is
        // part of what the adapter delegates to, so replacing the builder itself would test less.
        CloneUrlBuilder cloneUrlBuilder = new CloneUrlBuilder(new RepositoryEndpointPort() {
            @Override
            public String restScheme() {
                return "https";
            }

            @Override
            public String serviceHost() {
                return "example.invalid";
            }
        });

        repositoryAdapter = new RepositoryJpaPersistenceAdapter(
                repositories, users, organizations, organizationMembers, cloneUrlBuilder);
        memberAdapter = new RepositoryMemberJpaPersistenceAdapter(members);
        branchWriteAdapter = new BranchJpaRepositoryAdapter(branches);
        branchQueryAdapter = new BranchJpaQueryAdapter(branches);

        suffix = "jpa72" + Long.toString(System.nanoTime(), 36);
    }

    @AfterEach
    void tearDown() {
        if (suffix == null || jdbcTemplate == null) {
            return;
        }
        jdbcTemplate.update("delete from BRANCH where NAME like ?", suffix + "%");
        jdbcTemplate.update("delete from REPOSITORY_MEMBER where REPOSITORY_ID in "
                + "(select ID from REPOSITORY where PATH like ?)", "%" + suffix + "%");
        jdbcTemplate.update("delete from REPOSITORY where PATH like ?", "%" + suffix + "%");
        jdbcTemplate.update("delete from ORGANIZE_MEMBER where ORGANIZE_ID in "
                + "(select ID from ORGANIZE where NAME like ?)", suffix + "%");
        jdbcTemplate.update("delete from ORGANIZE where NAME like ?", suffix + "%");
        jdbcTemplate.update("delete from USER where USERNAME like ?", suffix + "%");
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void persistsReferenceSliceAgainstMariaDb() {
        Long userId = transactionTemplate.execute(status -> insertUser().getId());
        Long organizeId = transactionTemplate.execute(status -> insertOrganization().getId());

        Long publicRepoId = transactionTemplate.execute(status ->
                insertRepository("PUBLIC", "USER", userId, suffix + "-pub").getId());
        Long privateRepoId = transactionTemplate.execute(status ->
                insertRepository("PRIVATE", "USER", userId, suffix + "-priv").getId());
        Long orgRepoId = transactionTemplate.execute(status ->
                insertRepository("PRIVATE", "ORGANIZATION", organizeId, suffix + "-org").getId());

        // REPOSITORY_TYPE is read-only in the entity, so the database default must have filled it.
        assertThat(jdbcTemplate.queryForObject(
                "select REPOSITORY_TYPE from REPOSITORY where ID = ?", String.class, publicRepoId))
                .as("the column is NOT NULL DEFAULT 'GIT' and the entity never writes it")
                .isEqualTo("GIT");

        transactionTemplate.executeWithoutResult(status ->
                assertThat(repositoryAdapter.findById(RepositoryId.of(publicRepoId)))
                        .isPresent()
                        .get()
                        .satisfies(repository -> {
                            assertThat(repository.getPath().getValue()).isEqualTo(suffix + "-pub");
                            assertThat(repository.getVisibility().name()).isEqualTo("PUBLIC");
                        }));

        transactionTemplate.executeWithoutResult(status -> {
            List<RepositoryResult> anonymous = repositoryAdapter.loadVisibleRepositories(null);
            assertThat(anonymous).extracting(RepositoryResult::id).contains(publicRepoId);
            assertThat(anonymous).extracting(RepositoryResult::id).doesNotContain(privateRepoId, orgRepoId);
        });

        transactionTemplate.executeWithoutResult(status -> {
            List<RepositoryResult> own = repositoryAdapter.loadVisibleRepositories(userId);
            assertThat(own).extracting(RepositoryResult::id)
                    .as("a requester sees their own private repositories")
                    .contains(publicRepoId, privateRepoId);
            assertThat(own).extracting(RepositoryResult::id)
                    .as("but not an organization's, until they are a member")
                    .doesNotContain(orgRepoId);
        });

        transactionTemplate.executeWithoutResult(status -> insertOrganizationMember(organizeId, userId));

        transactionTemplate.executeWithoutResult(status ->
                assertThat(repositoryAdapter.loadVisibleRepositories(userId))
                        .extracting(RepositoryResult::id)
                        .as("membership is what unlocks the organization's private repositories, and it "
                                + "is resolved through the collaboration JPA repository")
                        .contains(orgRepoId));

        transactionTemplate.executeWithoutResult(status ->
                assertThat(repositoryAdapter.loadRepositoryByPath(suffix + "-user", suffix + "-pub"))
                        .as("namespace resolution goes user table first, then organization")
                        .isPresent()
                        .get()
                        .extracting(RepositoryResult::id)
                        .isEqualTo(publicRepoId));

        transactionTemplate.executeWithoutResult(status -> {
            RepositoryMember saved = memberAdapter.save(RepositoryMember.create(
                    RepositoryId.of(publicRepoId), RepositoryMemberUserId.of(userId),
                    RepositoryMemberRole.MAINTAINER, LocalDateTime.now()));
            assertThat(saved.getRole()).isEqualTo(RepositoryMemberRole.MAINTAINER);
        });

        transactionTemplate.executeWithoutResult(status ->
                assertThat(memberAdapter.existsByRepositoryIdAndUserId(
                        RepositoryId.of(publicRepoId), RepositoryMemberUserId.of(userId))).isTrue());

        transactionTemplate.executeWithoutResult(status ->
                branchWriteAdapter.save(Branch.create(publicRepoId, suffix + "-main", false, true, true)));

        transactionTemplate.executeWithoutResult(status -> {
            assertThat(branchQueryAdapter.findByRepositoryIdAndName(publicRepoId, suffix + "-main"))
                    .isPresent()
                    .get()
                    .satisfies(branch -> {
                        assertThat(branch.ciEnabled()).isTrue();
                        assertThat(branch.defaultBranch()).isTrue();
                        assertThat(branch.locked()).isFalse();
                    });
            assertThat(jdbcTemplate.queryForObject(
                    "select CREATED_AT is not null from BRANCH where REPOSITORY_ID = ? and NAME = ?",
                    Boolean.class, publicRepoId, suffix + "-main"))
                    .as("BRANCH.CREATED_AT is database-owned and the entity never writes it")
                    .isTrue();
        });

        transactionTemplate.executeWithoutResult(status ->
                branchWriteAdapter.delete(Branch.create(publicRepoId, suffix + "-main")));

        transactionTemplate.executeWithoutResult(status ->
                assertThat(branchQueryAdapter.findAllByRepositoryId(publicRepoId))
                        .as("delete is by (repositoryId, name), the branch's domain identity")
                        .isEmpty());
    }

    private UserJpaEntity insertUser() {
        LocalDateTime now = LocalDateTime.now();
        return users.save(new UserJpaEntity(null, suffix + "-user", suffix + "@example.invalid",
                suffix, null, "USER", "ACTIVE", null, now, now));
    }

    private OrganizeJpaEntity insertOrganization() {
        LocalDateTime now = LocalDateTime.now();
        // OWNER_ID is NOT NULL in the DDL and is not what this test is about, so it gets a value
        // rather than a null that would fail the insert for an unrelated reason.
        return organizations.save(new OrganizeJpaEntity(null, suffix + "-org", suffix + "-org",
                "repository jpa evidence", 1L, now, now));
    }

    private void insertOrganizationMember(Long organizeId, Long userId) {
        organizationMembers.save(new OrganizeMemberJpaEntity(
                null, organizeId, userId, "OWNER", LocalDateTime.now()));
    }

    private RepositoryJpaEntity insertRepository(String visibility, String ownerType, Long ownerId, String path) {
        RepositoryJpaEntity entity = new RepositoryJpaEntity();
        entity.setName(path);
        entity.setPath(path);
        entity.setOwnerType(ownerType);
        entity.setOwnerId(ownerId);
        entity.setClonePath("/" + path + ".git");
        entity.setDefaultBranch("main");
        entity.setVisibility(visibility);
        entity.setStatus("REGISTERED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return JpaMariaDbTestSupport.repository(emf, RepositoryJpaRepository.class).save(entity);
    }
}
