package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import io.jgitkins.server.persistence.jpa.JpaMariaDbTestSupport;
import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
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
 * The identity slice exercised end to end against real MariaDB.
 *
 * <p>The unit tests prove mapping; this proves the rows actually land in the real schema through
 * Hibernate. Generated identity, the enum-name columns, the {@code tinyint(1)} boolean, and
 * newest-first lookup order are all places where H2 in MariaDB mode and InnoDB can diverge, so they
 * are checked rather than assumed.
 *
 * <p>Skips when the database is unreachable, in which case the slice is unverified rather than
 * satisfied.
 */
class IdentityJpaMariaDbIntegrationTest {

    private LocalContainerEntityManagerFactoryBean factoryBean;
    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private UserJpaPersistenceAdapter userAdapter;
    private UserCredentialJpaPersistenceAdapter credentialAdapter;
    private UserIdentityJpaPersistenceAdapter identityAdapter;
    private String username;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = JpaMariaDbTestSupport.dataSource();
        factoryBean = JpaMariaDbTestSupport.entityManagerFactory(dataSource, "identity-reference-slice", "io.jgitkins.server.identity.access.adapter.out.persistence.jpa");
        EntityManagerFactory emf = factoryBean.getObject();

        transactions = new TransactionTemplate(new JpaTransactionManager(emf));
        userAdapter = new UserJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, UserJpaRepository.class));
        credentialAdapter = new UserCredentialJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, UserCredentialJpaRepository.class));
        identityAdapter = new UserIdentityJpaPersistenceAdapter(
                JpaMariaDbTestSupport.repository(emf, UserIdentityJpaRepository.class));

        jdbc = new JdbcTemplate(dataSource);
        username = "jpa-identity-" + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        if (jdbc != null && username != null) {
            jdbc.update("delete from USER_CREDENTIALS where USER_ID in (select ID from USER where USERNAME = ?)", username);
            jdbc.update("delete from USER_IDENTITIES where USER_ID in (select ID from USER where USERNAME = ?)", username);
            jdbc.update("delete from USER where USERNAME = ?", username);
        }
        if (factoryBean != null) {
            factoryBean.destroy();
        }
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void persistsReferenceSliceAgainstMariaDb() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Long userId = transactions.execute(status -> {
            User saved = userAdapter.save(User.rehydrate(null, username, username + "@example.com",
                    "Display", "https://avatar", UserAuthority.USER, UserStatus.ACTIVE, null, now, now));
            assertThat(saved.getId())
                    .as("MariaDB assigns the identity; the adapter must return it or callers cannot "
                            + "address the row they just wrote")
                    .isNotNull();
            return saved.getId();
        });

        assertThat(jdbc.queryForObject("select AUTHORITY from USER where ID = ?", String.class, userId))
                .as("AUTHORITY is stored as the enum name, matching MyBatis")
                .isEqualTo("USER");

        transactions.executeWithoutResult(status -> {
            assertThat(userAdapter.findByUsername("  " + username + "  "))
                    .as("lookups trim their input")
                    .isPresent();
            assertThat(userAdapter.findByEmail(username + "@example.com")).isPresent();
            assertThat(userAdapter.findUserIdByUsername(username)).contains(userId);
            assertThat(userAdapter.findUsernameById(userId)).contains(username);
            assertThat(userAdapter.existsByUsername(username)).isTrue();
            assertThat(userAdapter.findUserDetailsById(userId))
                    .hasValueSatisfying(result -> assertThat(result.status()).isEqualTo("ACTIVE"));
            assertThat(userAdapter.findByIdForUpdate(userId))
                    .as("the locked read must return the row, not just acquire a lock")
                    .isPresent();
        });

        transactions.executeWithoutResult(status -> {
            credentialAdapter.save(UserCredential.rehydrate(
                    null, userId, "LOCAL", "ci-token", "for CI", "hash-1", now, now));
            credentialAdapter.save(UserCredential.rehydrate(
                    null, userId, "LOCAL", "deploy-token", "for deploys", "hash-2", now, now));
            identityAdapter.save(UserIdentity.rehydrate(
                    null, userId, "google", "sub-" + userId, username + "@example.com", true,
                    "Display", null, now, now));
        });

        transactions.executeWithoutResult(status -> {
            assertThat(credentialAdapter.findAllByUserIdAndProvider(userId, "LOCAL")).hasSize(2);
            assertThat(credentialAdapter.findByUserIdAndProvider(userId, "LOCAL"))
                    .as("the single-result lookup takes the newest row, matching the MyBatis "
                            + "order by id desc limit 1")
                    .hasValueSatisfying(c -> assertThat(c.getName()).isEqualTo("deploy-token"));
            assertThat(identityAdapter.findByProvider("google", "sub-" + userId))
                    .hasValueSatisfying(i -> {
                        assertThat(i.getUserId()).isEqualTo(userId);
                        assertThat(i.isEmailVerified())
                                .as("EMAIL_VERIFIED is tinyint(1) in the schema and boolean in the "
                                        + "domain; the round trip must preserve true")
                                .isTrue();
                    });
            assertThat(identityAdapter.findAllByUserId(userId)).hasSize(1);
        });

        Long credentialId = jdbc.queryForObject(
                "select ID from USER_CREDENTIALS where USER_ID = ? order by ID desc limit 1", Long.class, userId);
        transactions.executeWithoutResult(status ->
                credentialAdapter.deleteByIdAndUserId(credentialId, userId));
        transactions.executeWithoutResult(status ->
                assertThat(credentialAdapter.findAllByUserIdAndProvider(userId, "LOCAL")).hasSize(1));

        transactions.executeWithoutResult(status ->
                credentialAdapter.deleteByIdAndUserId(credentialId, userId + 999_999L));
        transactions.executeWithoutResult(status ->
                assertThat(credentialAdapter.findAllByUserIdAndProvider(userId, "LOCAL"))
                        .as("delete keeps both predicates: the user id is the authorization check, so a "
                                + "mismatched owner must delete nothing")
                        .hasSize(1));
    }
}
