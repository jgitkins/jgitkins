package io.jgitkins.server.identity.access.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * When a new provider identity may attach itself to an account that already exists.
 *
 * <p>Signing in with a provider the account has never used takes one of two paths: attach to the
 * account that holds this email address, or create a new one. The first is how a person keeps a
 * single account across providers. It is also, if the address was never checked, how someone else
 * takes that account over — assert the victim's address at any configured provider and the identity
 * attaches, and the JWT that comes back is the victim's.
 *
 * <p>{@code emailVerified} rode along on the command since it was introduced and was never read.
 * These tests are what reading it means.
 */
class UserServiceAccountLinkingTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserIdentityPersistencePort identityPort = mock(UserIdentityPersistencePort.class);
    private final UsernameAllocator allocator = mock(UsernameAllocator.class);
    private final UserService service =
            new UserService(userRepository, identityPort, allocator, new UserProfileUpdater());

    private final User existing =
            User.createWithStatus("victim", "victim@corp.test", "Victim", null, UserStatus.ACTIVE).withId(7L);

    @Test
    void verifiedEmailAttachesTheNewIdentityToTheExistingAccount() {
        when(identityPort.findByProvider("google", "attacker-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victim@corp.test")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.loginOrSignUp(new UserLoginOrSignUpCommand(
                "google", "attacker-sub", "victim@corp.test", true, "Victim", null));

        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    void unverifiedEmailDoesNotReachTheAccountThatHoldsThatAddress() {
        // The rule. Without it, "I am victim@corp.test" is enough to become that account, and
        // whether it is enough depends on a provider we do not control.
        when(identityPort.findByProvider("google", "attacker-sub")).thenReturn(Optional.empty());
        when(allocator.deriveBaseUsername(anyString(), anyString(), anyString())).thenReturn("victim");
        when(allocator.allocateUniqueUsername(anyString(), anyString())).thenReturn("victim-2");
        // The real repository assigns the id; UserIdentity.create needs it on the way back out.
        when(userRepository.save(any(User.class)))
                .thenAnswer(call -> ((User) call.getArgument(0)).withId(99L));

        User result = service.loginOrSignUp(new UserLoginOrSignUpCommand(
                "google", "attacker-sub", "victim@corp.test", false, "Not The Victim", null));

        // A separate account, and the existing one was never so much as looked up -- so it was also
        // never rewritten with the name and avatar this request carried.
        verify(userRepository, never()).findByEmail(anyString());
        assertThat(result.getId()).isEqualTo(99L).isNotEqualTo(existing.getId());
        assertThat(result.getUsername()).isEqualTo("victim-2");
    }
}
