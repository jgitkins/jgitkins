package io.jgitkins.server.identity.access.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.JGitkinsServerApplication;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.identity.access.domain.vo.Username;
import io.jgitkins.server.identity.access.adapter.out.persistence.support.UserDomainMapper;
import io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserEntityMbgMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = JGitkinsServerApplication.class)
@ActiveProfiles("identity-access-integration")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class UserActivationPersistenceIntegrationTest {

    private static Path runtimeVolume;

    @Autowired private UserPersistenceAdapter adapter;
    @Autowired private UserDomainMapper userDomainMapper;
    @Autowired private UserEntityMbgMapper userEntityMbgMapper;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void fixtureProperties(DynamicPropertyRegistry registry) {
        try {
            runtimeVolume = Files.createTempDirectory("identity-access-integration-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        registry.add("jgitkins.server.runtime.volume", () -> runtimeVolume.toString());
    }

    @AfterAll
    void removeRuntimeVolume() throws IOException {
        if (runtimeVolume != null && Files.exists(runtimeVolume)) {
            try (var paths = Files.walk(runtimeVolume)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }

    @Test
    void activatesPendingUserThroughRealPersistenceAdapter() {
        assertThat(userDomainMapper).isNotNull();
        assertThat(userEntityMbgMapper).isNotNull();

        User pending = adapter.findById(1001L).orElseThrow();
        LocalDateTime createdAt = pending.getCreatedAt();
        LocalDateTime lastLoginAt = pending.getLastLoginAt();
        User activated = adapter.save(pending.activateWithUsername(Username.from("activated-user")));
        User reloaded = adapter.findById(1001L).orElseThrow();

        assertThat(activated.getUsername()).isEqualTo("activated-user");
        assertThat(reloaded.getUsername()).isEqualTo("activated-user");
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getLastLoginAt()).isEqualTo(lastLoginAt);
        assertThat(reloaded.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    void normalizesLegacyPendingUsernameBeforeActivation() {
        User legacy = adapter.findById(1002L).orElseThrow();
        assertThat(legacy.getStatus()).isEqualTo(UserStatus.PENDING);

        User activated = adapter.save(legacy.activateWithUsername(Username.from("legacy-activated")));
        User reloaded = adapter.findById(1002L).orElseThrow();

        assertThat(activated.getUsername()).isEqualTo("legacy-activated");
        assertThat(reloaded.getUsername()).isEqualTo("legacy-activated");
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reloaded.getLastLoginAt()).isEqualTo(LocalDateTime.of(2025, 12, 31, 23, 0));
        assertThat(reloaded.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(reloaded.getUpdatedAt()).isAfter(reloaded.getCreatedAt());
    }

    @Test
    void insertsSeparateUserWithGeneratedId() {
        User inserted = adapter.save(User.createWithStatus(
                "generated-user", "generated@example.test", "Generated User", null, UserStatus.PENDING));

        assertThat(inserted.getId()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT ID FROM USER WHERE USERNAME = 'generated-user'", Long.class))
                .isEqualTo(inserted.getId());
    }
}
