package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Mapping between the identity domain types and the JPA entities, without a database.
 *
 * <p>The cases worth asserting are the ones where the two providers could silently disagree:
 * {@code AUTHORITY} and {@code STATUS} round-tripping as enum names, the fallbacks a null column
 * takes, whitespace trimming on the lookups, and blank input never reaching the repository. Each of
 * those is a behaviour {@code UserPersistenceAdapter} has today, not a JPA convention.
 */
class IdentityJpaMappingTest {

    private final UserJpaRepository userJpaRepository = Mockito.mock(UserJpaRepository.class);
    private final UserCredentialJpaRepository credentialJpaRepository =
            Mockito.mock(UserCredentialJpaRepository.class);
    private final UserIdentityJpaRepository identityJpaRepository =
            Mockito.mock(UserIdentityJpaRepository.class);

    private final UserJpaPersistenceAdapter userAdapter = new UserJpaPersistenceAdapter(userJpaRepository);
    private final UserCredentialJpaPersistenceAdapter credentialAdapter =
            new UserCredentialJpaPersistenceAdapter(credentialJpaRepository);
    private final UserIdentityJpaPersistenceAdapter identityAdapter =
            new UserIdentityJpaPersistenceAdapter(identityJpaRepository);

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 27, 12, 0);

    @Test
    void mapsUserCredentialsAndIdentities() {
        Mockito.when(userJpaRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
        User user = User.rehydrate(5L, "alice", "alice@example.com", "Alice", "https://avatar",
                UserAuthority.ADMIN, UserStatus.ACTIVE, NOW, NOW, NOW);

        User saved = userAdapter.save(user);

        ArgumentCaptor<UserJpaEntity> captor = ArgumentCaptor.forClass(UserJpaEntity.class);
        Mockito.verify(userJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthority())
                .as("AUTHORITY is a varchar holding the enum name, matching UserDomainMapper; mapping it "
                        + "as @Enumerated would tie the column to Java enum ordering")
                .isEqualTo("ADMIN");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getAuthority()).isEqualTo(UserAuthority.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getUsername()).isEqualTo("alice");

        // A null authority column falls back to USER, exactly as the MyBatis mapper does.
        Mockito.when(userJpaRepository.findById(6L)).thenReturn(Optional.of(new UserJpaEntity(
                6L, "bob", null, null, null, null, null, null, NOW, NOW)));
        assertThat(userAdapter.findById(6L)).hasValueSatisfying(found -> {
            assertThat(found.getAuthority()).isEqualTo(UserAuthority.USER);
            assertThat(found.getStatus()).isEqualTo(UserStatus.fromNullable(null));
        });

        Mockito.when(credentialJpaRepository.findAllByUserIdAndProviderOrderByIdDesc(5L, "LOCAL"))
                .thenReturn(List.of(new UserCredentialJpaEntity(
                        1L, 5L, "LOCAL", "ci-token", "for CI", "hash", NOW, NOW)));
        List<UserCredential> credentials = credentialAdapter.findAllByUserIdAndProvider(5L, "LOCAL");
        assertThat(credentials).hasSize(1);
        assertThat(credentials.get(0).getName()).isEqualTo("ci-token");
        assertThat(credentials.get(0).getPasswordHash()).isEqualTo("hash");

        Mockito.when(identityJpaRepository.findFirstByProviderNameAndProviderSubOrderByIdDesc("google", "sub-1"))
                .thenReturn(Optional.of(new UserIdentityJpaEntity(
                        2L, 5L, "google", "sub-1", "alice@example.com", true, "Alice", null, NOW, NOW)));
        assertThat(identityAdapter.findByProvider("  google  ", " sub-1 "))
                .as("provider lookups trim their input, matching the MyBatis adapter; resolving the "
                        + "wrong row here logs someone into the wrong account")
                .hasValueSatisfying(identity -> {
                    assertThat(identity.getUserId()).isEqualTo(5L);
                    assertThat(identity.isEmailVerified()).isTrue();
                });
    }

    @Test
    void blankAndNullInputNeverReachesTheRepository() {
        assertThat(userAdapter.findByEmail(" ")).isEmpty();
        assertThat(userAdapter.findByUsername(null)).isEmpty();
        assertThat(userAdapter.findById(null)).isEmpty();
        assertThat(userAdapter.findByIdForUpdate(null)).isEmpty();
        assertThat(userAdapter.findUserIdByUsername("")).isEmpty();
        assertThat(userAdapter.findUsernameById(null)).isEmpty();
        assertThat(userAdapter.existsByUsername(" ")).isFalse();
        Mockito.verifyNoInteractions(userJpaRepository);

        assertThat(credentialAdapter.findByUserIdAndProvider(null, "LOCAL")).isEmpty();
        assertThat(credentialAdapter.findAllByUserIdAndProvider(1L, null)).isEmpty();
        credentialAdapter.deleteByIdAndUserId(null, 1L);
        Mockito.verifyNoInteractions(credentialJpaRepository);

        assertThat(identityAdapter.findByProvider("google", " ")).isEmpty();
        assertThat(identityAdapter.findAllByUserId(null)).isEmpty();
        Mockito.verifyNoInteractions(identityJpaRepository);
    }
}
