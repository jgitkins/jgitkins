package io.jgitkins.server.collaboration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.collaboration.adapter.out.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * {@code findOrganizeIdsByUserId} must answer the same thing under both providers.
 *
 * <p>The port is new, and it decides a visibility filter: {@code repository} uses it to choose which
 * organization-owned repositories a requester may see. Two implementations of one such decision that
 * can disagree is the shape this repository has been bitten by twice already -- a route guard that
 * enumerated half the chain (41af03d), and a test named {@code UnderBothProviders} that constructed
 * one of them (b5a7fcb). The existing coverage would have repeated it: {@code loadVisibleRepositories}
 * is exercised against real MariaDB only through {@code RepositoryJpaMariaDbIntegrationTest}, so the
 * MyBatis half of this port had no test at all.
 *
 * <p>Both adapters are built over one database and asked the same question, and the assertion is that
 * their answers are equal as well as correct. Asserting only correctness twice would let a shared
 * misreading of the schema pass; asserting equality alone would let both be wrong together, so this
 * does both.
 *
 * <p>Runs against real MariaDB via {@link JpaMariaDbTestSupport} rather than the suite's H2, because
 * the question is a read against {@code ORGANIZE_MEMBER} as {@code ddl.sql} declares it -- including
 * {@code UK_ORGANIZE_MEMBER_USER}, which is why neither implementation can return a duplicate.
 */
class OrganizeIdsByUserBothProvidersMariaDbTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean factoryBean;
    private OrganizeMembershipQueryPort mybatis;
    private OrganizeMembershipQueryPort jpa;

    private long userId;
    private String marker;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = JpaMariaDbTestSupport.dataSource();
        jdbc = new JdbcTemplate(dataSource);

        SqlSessionTemplate session = new SqlSessionTemplate(sqlSessionFactory(dataSource));
        mybatis = new OrganizeMemberPersistenceAdapter(
                session.getMapper(OrganizeMemberEntityMbgMapper.class),
                org.mapstruct.factory.Mappers.getMapper(OrganizeMemberDomainMapper.class));

        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "organize-ids-by-user",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();
        jpa = new OrganizeMemberJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, OrganizeMemberJpaRepository.class));

        marker = "ids-by-user-" + Long.toString(System.nanoTime(), 36);
        userId = 900_000_000L + (System.nanoTime() % 1_000_000L);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from ORGANIZE_MEMBER where USER_ID = ?", userId);
        jdbc.update("delete from ORGANIZE where PATH like ?", marker + "%");
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    void bothProvidersReturnEveryOrganizationTheUserBelongsTo() {
        long first = insertOrganize("a");
        long second = insertOrganize("b");
        insertOrganize("c");                       // exists, user is not a member
        addMember(first, "MEMBER");
        addMember(second, "OWNER");                // role must not filter the answer

        assertThat(mybatis.findOrganizeIdsByUserId(userId))
                .as("membership, not ownership: both roles count and the third organization does not")
                .containsExactlyInAnyOrder(first, second);
        assertThat(jpa.findOrganizeIdsByUserId(userId))
                .containsExactlyInAnyOrder(first, second);
        assertThat(sorted(jpa)).isEqualTo(sorted(mybatis));
    }

    @Test
    void bothProvidersAnswerEmptyForAUserInNoOrganization() {
        insertOrganize("lonely");                  // an organization exists, the user is not in it

        assertThat(mybatis.findOrganizeIdsByUserId(userId)).isEmpty();
        assertThat(jpa.findOrganizeIdsByUserId(userId)).isEmpty();
    }

    @Test
    void bothProvidersAnswerEmptyForNull() {
        // repository calls this with the requester id, and an anonymous requester is null. Throwing
        // here would turn every anonymous repository list into a 500.
        assertThat(mybatis.findOrganizeIdsByUserId(null)).isEmpty();
        assertThat(jpa.findOrganizeIdsByUserId(null)).isEmpty();
    }

    private List<Long> sorted(OrganizeMembershipQueryPort port) {
        return port.findOrganizeIdsByUserId(userId).stream().sorted().toList();
    }

    private long insertOrganize(String suffix) {
        String path = marker + "-" + suffix;
        jdbc.update("insert into ORGANIZE (NAME, PATH, OWNER_ID) values (?, ?, ?)", path, path, userId);
        return jdbc.queryForObject("select ID from ORGANIZE where PATH = ?", Long.class, path);
    }

    private void addMember(long organizeId, String role) {
        jdbc.update("insert into ORGANIZE_MEMBER (ORGANIZE_ID, USER_ID, ROLE) values (?, ?, ?)",
                organizeId, userId, role);
    }

    private static SqlSessionFactory sqlSessionFactory(DriverManagerDataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new ClassPathResource("mapper/mbg/OrganizeMemberEntityMbgMapper.xml"));
        factoryBean.setConfiguration(new org.apache.ibatis.session.Configuration());
        return factoryBean.getObject();
    }
}
