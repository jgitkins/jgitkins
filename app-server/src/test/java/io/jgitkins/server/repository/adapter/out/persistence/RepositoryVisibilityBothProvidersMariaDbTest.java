package io.jgitkins.server.repository.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizePersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeMemberDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaPersistenceAdapter;
import io.jgitkins.server.identity.access.adapter.out.persistence.jpa.UserJpaRepository;
import io.jgitkins.server.identity.access.adapter.out.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserDomainMapper;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationMembershipAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationNamespaceAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.UserNamespaceAclAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaPersistenceAdapter;
import io.jgitkins.server.repository.adapter.out.persistence.jpa.RepositoryJpaRepository;
import io.jgitkins.server.repository.adapter.out.persistence.mapper.RepositoryEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryDomainMapper;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.RepositoryEndpointPort;
import io.jgitkins.server.repository.application.support.CloneUrlBuilder;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * The repository visibility filter and namespace resolution, under both providers, over one database.
 *
 * <p>Written because the MyBatis half of the persistence-decoupling refactor had no behavioural test.
 * f0f78eb rewrote both adapters' three cross-context reads -- username to id, namespace to organization
 * id, user to organization ids -- to go through ports, and only the JPA adapter's MariaDB test was
 * rewired to exercise them. Every test naming {@code RepositoryPersistenceAdapter} asserted wiring
 * against mocks. That is the shape this repository has been bitten by three times: 41af03d (a route
 * guard that enumerated half the chain), b5a7fcb (a test named UnderBothProviders that constructed one),
 * and the import scan whose target package had been renamed away.
 *
 * <p>Both adapters are assembled the way production assembles them -- the real ACL adapters over the
 * real collaboration and identity adapters of the matching provider -- and asked the same questions
 * about the same rows. Answers must be equal as well as correct: asserting correctness twice would let
 * a shared misreading pass, and asserting equality alone would let both be wrong together.
 */
class RepositoryVisibilityBothProvidersMariaDbTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean factoryBean;

    private RepositoryPersistenceAdapter mybatis;
    private RepositoryJpaPersistenceAdapter jpa;

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

        SqlSessionTemplate session = new SqlSessionTemplate(sqlSessionFactory(dataSource));
        mybatis = new RepositoryPersistenceAdapter(
                new UserNamespaceAclAdapter(new UserPersistenceAdapter(
                        session.getMapper(UserEntityMbgMapper.class),
                        org.mapstruct.factory.Mappers.getMapper(UserDomainMapper.class))),
                new OrganizationNamespaceAclAdapter(new OrganizePersistenceAdapter(
                        session.getMapper(OrganizeEntityMbgMapper.class),
                        org.mapstruct.factory.Mappers.getMapper(OrganizeDomainMapper.class))),
                new OrganizationMembershipAclAdapter(new OrganizeMemberPersistenceAdapter(
                        session.getMapper(OrganizeMemberEntityMbgMapper.class),
                        org.mapstruct.factory.Mappers.getMapper(OrganizeMemberDomainMapper.class))),
                session.getMapper(RepositoryEntityMbgMapper.class),
                cloneUrlBuilder(),
                org.mapstruct.factory.Mappers.getMapper(RepositoryDomainMapper.class));

        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "repository-visibility-parity",
                "io.jgitkins.server.repository.adapter.out.persistence.jpa",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa",
                "io.jgitkins.server.identity.access.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();
        jpa = new RepositoryJpaPersistenceAdapter(
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
    void bothProvidersHideWhatAnAnonymousVisitorMayNotSee() {
        List<Long> fromMybatis = ids(mybatis.loadVisibleRepositories(null));
        List<Long> fromJpa = ids(jpa.loadVisibleRepositories(null));

        assertThat(fromMybatis).contains(publicRepoId).doesNotContain(privateRepoId, orgRepoId);
        assertThat(fromJpa).contains(publicRepoId).doesNotContain(privateRepoId, orgRepoId);
        assertThat(fromMybatis).isEqualTo(fromJpa);
    }

    @Test
    void bothProvidersShowAnOwnerTheirOwnPrivateRepositoriesAndNoOrganizationsTheyAreNotIn() {
        List<Long> fromMybatis = ids(mybatis.loadVisibleRepositories(userId));
        List<Long> fromJpa = ids(jpa.loadVisibleRepositories(userId));

        assertThat(fromMybatis).contains(publicRepoId, privateRepoId).doesNotContain(orgRepoId);
        assertThat(fromJpa).contains(publicRepoId, privateRepoId).doesNotContain(orgRepoId);
        assertThat(fromMybatis).isEqualTo(fromJpa);
    }

    @Test
    void membershipUnlocksTheOrganizationsRepositoriesUnderBothProviders() {
        // The read that goes through OrganizeMembershipQueryPort#findOrganizeIdsByUserId, the port this
        // refactor added. Before membership exists, the organization's private repository is invisible.
        assertThat(ids(mybatis.loadVisibleRepositories(userId))).doesNotContain(orgRepoId);
        assertThat(ids(jpa.loadVisibleRepositories(userId))).doesNotContain(orgRepoId);

        jdbc.update("insert into ORGANIZE_MEMBER (ORGANIZE_ID, USER_ID, ROLE) values (?, ?, 'MEMBER')",
                organizeId, userId);

        List<Long> fromMybatis = ids(mybatis.loadVisibleRepositories(userId));
        List<Long> fromJpa = ids(jpa.loadVisibleRepositories(userId));
        assertThat(fromMybatis)
                .as("membership is what unlocks it, and it is resolved through the collaboration port")
                .contains(orgRepoId);
        assertThat(fromJpa).contains(orgRepoId);
        assertThat(fromMybatis).isEqualTo(fromJpa);
    }

    @Test
    void bothProvidersResolveAUserNamespaceThroughTheIdentityPort() {
        Optional<RepositoryResult> fromMybatis =
                mybatis.loadRepositoryByPath(suffix + "-user", suffix + "-pub");
        Optional<RepositoryResult> fromJpa = jpa.loadRepositoryByPath(suffix + "-user", suffix + "-pub");

        assertThat(fromMybatis).isPresent().get().extracting(RepositoryResult::id).isEqualTo(publicRepoId);
        assertThat(fromJpa).isPresent().get().extracting(RepositoryResult::id).isEqualTo(publicRepoId);
    }

    @Test
    void bothProvidersResolveAnOrganizationNamespaceThroughTheCollaborationPort() {
        Optional<RepositoryResult> fromMybatis =
                mybatis.loadRepositoryByPath(suffix + "-org", suffix + "-org-repo");
        Optional<RepositoryResult> fromJpa =
                jpa.loadRepositoryByPath(suffix + "-org", suffix + "-org-repo");

        assertThat(fromMybatis).isPresent().get().extracting(RepositoryResult::id).isEqualTo(orgRepoId);
        assertThat(fromJpa).isPresent().get().extracting(RepositoryResult::id).isEqualTo(orgRepoId);
    }

    @Test
    void aNamespaceThatCannotBeAnOrganizationNameIsEmptyUnderBothProvidersRatherThanAnError() {
        // The behavioural risk of routing these adapters through OrganizationNamespacePort:
        // OrganizeName.from throws outside [A-Za-z0-9_-]+, and the namespace is a URL path segment.
        // Before the port, both ran a raw `where NAME = ?` that simply matched nothing.
        for (String namespace : List.of("has space", "dot.separated", "percent%20", "한글")) {
            assertThatCode(() -> mybatis.loadRepositoryByPath(namespace, suffix + "-pub"))
                    .as("MyBatis: %s must be a miss, not a 500", namespace)
                    .doesNotThrowAnyException();
            assertThatCode(() -> jpa.loadRepositoryByPath(namespace, suffix + "-pub"))
                    .as("JPA: %s must be a miss, not a 500", namespace)
                    .doesNotThrowAnyException();

            assertThat(mybatis.loadRepositoryByPath(namespace, suffix + "-pub")).isEmpty();
            assertThat(jpa.loadRepositoryByPath(namespace, suffix + "-pub")).isEmpty();
        }
    }

    @Test
    void bothProvidersReturnNothingForAUserThatDoesNotExist() {
        assertThat(mybatis.loadUserRepositories(suffix + "-nobody", null)).isEmpty();
        assertThat(jpa.loadUserRepositories(suffix + "-nobody", null)).isEmpty();
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

    private static SqlSessionFactory sqlSessionFactory(DriverManagerDataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new Resource[]{
                new ClassPathResource("mapper/mbg/RepositoryEntityMbgMapper.xml"),
                new ClassPathResource("mapper/mbg/OrganizeEntityMbgMapper.xml"),
                new ClassPathResource("mapper/mbg/OrganizeMemberEntityMbgMapper.xml"),
                new ClassPathResource("mapper/mbg/UserEntityMbgMapper.xml")});
        factoryBean.setConfiguration(new org.apache.ibatis.session.Configuration());
        return factoryBean.getObject();
    }
}
