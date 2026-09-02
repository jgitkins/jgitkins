package io.jgitkins.server.repository.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaRepository;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationMembershipAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationNamespaceAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.UserNamespaceAclAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaRepository;
import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * The repository visibility filter and namespace resolution, over one real database.
 *
 * <p>Was {@code RepositoryVisibilityBothProvidersMariaDbTest}. It asked both adapters the same
 * questions about the same rows and asserted the answers were equal as well as correct -- the
 * reasoning being that asserting correctness twice would let a shared misreading pass, and asserting
 * equality alone would let both be wrong together.
 *
 * <p><strong>The equality half is gone and that is a real reduction, not a tidy-up.</strong> It was
 * load-bearing while a rollback existed: the two adapters had to agree, because an operator could
 * switch between them. app-server now runs JPA for every slice and the rollback has been given up
 * deliberately, so there is no second answer to compare against. What survives is the correctness
 * half, which is the half that was worth having anyway -- these are the rules that decide whether an
 * anonymous visitor can see a private repository.
 *
 * <p>The original was written because the MyBatis half of the persistence-decoupling refactor had no
 * behavioural test at all: f0f78eb rewrote both adapters' three cross-context reads to go through
 * ports and only the JPA adapter's MariaDB test was rewired, while every test naming
 * {@code RepositoryPersistenceAdapter} asserted wiring against mocks. That gap is the reason these
 * assertions exist, and it is why they were carried over rather than deleted with the provider:
 * three times in this repository a test has been named for coverage it did not have -- 41af03d (a
 * route guard that enumerated half the chain), b5a7fcb (a test named UnderBothProviders that
 * constructed one), and a scan whose target package had been renamed away.
 *
 * <p>The adapter is still assembled the way production assembles it, with the real ACL adapters over
 * the real collaboration and identity adapters. Mocking those would turn this back into a wiring
 * test.
 */
class RepositoryVisibilityMariaDbTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean factoryBean;

    private RepositoryJpaPersistenceAdapter adapter;

    private String suffix;
    private long userId;
    private long organizeId;
    private long publicRepoId;
    private long privateRepoId;
    private long orgRepoId;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = JpaMariaDbTestSupport.dataSource();
        jdbc = new JdbcTemplate(dataSource);
        suffix = "vis" + Long.toString(System.nanoTime(), 36);

        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "repository-visibility",
                "io.jgitkins.server.repository.adapter.out.persistence.jpa",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa",
                "io.jgitkins.server.identity.access.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();
        adapter = new RepositoryJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, RepositoryJpaRepository.class),
                new UserNamespaceAclAdapter(new UserJpaPersistenceAdapter(
                        JpaMariaDbTestSupport.repository(emf, UserJpaRepository.class))),
                new OrganizationNamespaceAclAdapter(new OrganizeJpaPersistenceAdapter(
                        JpaMariaDbTestSupport.repository(emf, OrganizeJpaRepository.class))),
                new OrganizationMembershipAclAdapter(new OrganizeMemberJpaPersistenceAdapter(
                        JpaMariaDbTestSupport.repository(emf, OrganizeMemberJpaRepository.class))),
                cloneUrlBuilder());

        seed();
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null) {
            jdbc.update("delete from ORGANIZE_MEMBER where USER_ID = ?", userId);
            jdbc.update("delete from REPOSITORY where PATH like ?", suffix + "%");
            jdbc.update("delete from ORGANIZE where PATH like ?", suffix + "%");
            jdbc.update("delete from USER where USERNAME like ?", suffix + "%");
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void hidesWhatAnAnonymousVisitorMayNotSee() {
        assertThat(ids(adapter.loadVisibleRepositories(null)))
                .contains(publicRepoId)
                .doesNotContain(privateRepoId, orgRepoId);
    }

    @Test
    void showsAnOwnerTheirOwnPrivateRepositoriesAndNoOrganizationsTheyAreNotIn() {
        assertThat(ids(adapter.loadVisibleRepositories(userId)))
                .contains(publicRepoId, privateRepoId)
                .doesNotContain(orgRepoId);
    }

    @Test
    void membershipUnlocksTheOrganizationsRepositories() {
        // The read that goes through OrganizeMembershipQueryPort#findOrganizeIdsByUserId, the port this
        // refactor added. Before membership exists, the organization's private repository is invisible.
        assertThat(ids(adapter.loadVisibleRepositories(userId))).doesNotContain(orgRepoId);

        jdbc.update("insert into ORGANIZE_MEMBER (ORGANIZE_ID, USER_ID, ROLE) values (?, ?, 'MEMBER')",
                organizeId, userId);

        assertThat(ids(adapter.loadVisibleRepositories(userId)))
                .as("membership is what unlocks it, and it is resolved through the collaboration port")
                .contains(orgRepoId);
    }

    @Test
    void resolvesAUserNamespaceThroughTheIdentityPort() {
        Optional<RepositoryResult> found = adapter.loadRepositoryByPath(suffix + "-user", suffix + "-pub");

        assertThat(found).isPresent().get().extracting(RepositoryResult::id).isEqualTo(publicRepoId);
    }

    @Test
    void resolvesAnOrganizationNamespaceThroughTheCollaborationPort() {
        Optional<RepositoryResult> found =
                adapter.loadRepositoryByPath(suffix + "-org", suffix + "-org-repo");

        assertThat(found).isPresent().get().extracting(RepositoryResult::id).isEqualTo(orgRepoId);
    }

    @Test
    void aNamespaceThatCannotBeAnOrganizationNameIsEmptyRatherThanAnError() {
        // The behavioural risk of routing this adapter through OrganizationNamespacePort:
        // OrganizeName.from throws outside [A-Za-z0-9_-]+, and the namespace is a URL path segment.
        // Before the port, a raw `where NAME = ?` simply matched nothing.
        for (String namespace : List.of("has space", "dot.separated", "percent%20", "한글")) {
            assertThatCode(() -> adapter.loadRepositoryByPath(namespace, suffix + "-pub"))
                    .as("%s must be a miss, not a 500", namespace)
                    .doesNotThrowAnyException();

            assertThat(adapter.loadRepositoryByPath(namespace, suffix + "-pub")).isEmpty();
        }
    }

    @Test
    void returnsNothingForAUserThatDoesNotExist() {
        assertThat(adapter.loadUserRepositories(suffix + "-nobody", null)).isEmpty();
    }

    private void seed() {
        jdbc.update("insert into USER (USERNAME, EMAIL, DISPLAY_NAME, AUTHORITY, STATUS)"
                        + " values (?, ?, ?, 'USER', 'ACTIVE')",
                suffix + "-user", suffix + "@example.invalid", suffix);
        userId = jdbc.queryForObject("select ID from USER where USERNAME = ?", Long.class, suffix + "-user");

        // OWNER_ID is NOT NULL and is not what this test is about.
        jdbc.update("insert into ORGANIZE (NAME, PATH, OWNER_ID) values (?, ?, ?)",
                suffix + "-org", suffix + "-org", userId);
        organizeId = jdbc.queryForObject("select ID from ORGANIZE where PATH = ?", Long.class,
                suffix + "-org");

        publicRepoId = insertRepository("PUBLIC", "USER", userId, suffix + "-pub");
        privateRepoId = insertRepository("PRIVATE", "USER", userId, suffix + "-priv");
        orgRepoId = insertRepository("PRIVATE", "ORGANIZATION", organizeId, suffix + "-org-repo");
    }

    private long insertRepository(String visibility, String ownerType, long ownerId, String name) {
        jdbc.update("insert into REPOSITORY (NAME, PATH, OWNER_TYPE, OWNER_ID, VISIBILITY,"
                        + " DEFAULT_BRANCH, CLONE_PATH) values (?, ?, ?, ?, ?, 'main', ?)",
                name, name, ownerType, ownerId, visibility, "/" + name + ".git");
        return jdbc.queryForObject("select ID from REPOSITORY where PATH = ?", Long.class, name);
    }

    private static List<Long> ids(List<RepositoryResult> results) {
        return results.stream().map(RepositoryResult::id).sorted().toList();
    }

    private static CloneUrlBuilder cloneUrlBuilder() {
        return new CloneUrlBuilder(new RepositoryEndpointPort() {
            @Override
            public String restScheme() {
                return "https";
            }

            @Override
            public String serviceHost() {
                return "example.invalid";
            }
        });
    }
}
