package io.jgitkins.server.collaboration.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaPersistenceAdapter;
import io.jgitkins.server.collaboration.adapter.out.persistence.jpa.OrganizeMemberJpaRepository;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * What {@code findOrganizeIdsByUserId} answers, over one real database.
 *
 * <p>Was {@code OrganizeIdsByUserBothProvidersMariaDbTest}. It asked the MyBatis and JPA
 * implementations the same question and asserted their answers were equal as well as correct,
 * because the two differ in mechanism: JPA projects the column, MyBatis read rows through the
 * generated mapper. The equality assertion went with the MyBatis adapter -- there is no second
 * answer to compare against once app-server runs JPA everywhere and the rollback is given up.
 *
 * <p>The correctness assertions are the reason this file was carried over rather than deleted with
 * the provider. This port decides a visibility filter -- which organization-owned repositories a
 * requester sees -- so what it answers is a security question, not a mapping detail. Two of the three
 * cases here are the ones that would be easy to get wrong and hard to notice: role must not filter
 * the answer, and a null requester must come back empty rather than throw.
 */
class OrganizeIdsByUserMariaDbTest {

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private LocalContainerEntityManagerFactoryBean factoryBean;

    private OrganizeMembershipQueryPort port;

    private long userId;
    private String marker;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = JpaMariaDbTestSupport.dataSource();
        jdbc = new JdbcTemplate(dataSource);

        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "organize-ids-by-user",
                "io.jgitkins.server.collaboration.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();
        port = new OrganizeMemberJpaPersistenceAdapter(
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
    void returnsEveryOrganizationTheUserBelongsTo() {
        long first = insertOrganize("a");
        long second = insertOrganize("b");
        insertOrganize("c");                       // exists, user is not a member
        addMember(first, "MEMBER");
        addMember(second, "OWNER");                // role must not filter the answer

        assertThat(port.findOrganizeIdsByUserId(userId))
                .as("membership, not ownership: both roles count and the third organization does not")
                .containsExactlyInAnyOrder(first, second);
    }

    @Test
    void answersEmptyForAUserInNoOrganization() {
        insertOrganize("lonely");                  // an organization exists, the user is not in it

        assertThat(port.findOrganizeIdsByUserId(userId)).isEmpty();
    }

    @Test
    void answersEmptyForNull() {
        // repository calls this with the requester id, and an anonymous requester is null. Throwing
        // here would turn every anonymous repository list into a 500.
        assertThat(port.findOrganizeIdsByUserId(null)).isEmpty();
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
}
