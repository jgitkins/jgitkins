package io.jgitkins.server.identity.access.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
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
 *
 * <h2>What the first version of these tests missed</h2>
 *
 * <p>It asserted that an unverified address produces a separate account, with {@code save} mocked to
 * succeed. A mock cannot violate a unique index, and {@code USER.EMAIL} carries one
 * ({@code UK_USERS_EMAIL}), so the assertion described an outcome the database refuses: the insert
 * failed and the caller got a 500. The rule was correct and its consequence was untested, which is
 * the pattern to watch for here — a mocked persistence port makes any invariant that lives in the
 * schema invisible. {@link #unverifiedEmailOnATakenAddressIsRefusedWithConflict} is the replacement,
 * and it asserts the status the caller actually receives.
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
    void unverifiedEmailOnATakenAddressIsRefusedWithConflict() {
        // The rule. Without it, "I am victim@corp.test" is enough to become that account, and
        // whether it is enough depends on a provider we do not control.
        when(identityPort.findByProvider("google", "attacker-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("victim@corp.test")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.loginOrSignUp(new UserLoginOrSignUpCommand(
                "google", "attacker-sub", "victim@corp.test", false, "Not The Victim", null)))
                .isInstanceOf(ApplicationException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ApplicationException.class))
                .extracting(ApplicationException::getErrorCode)
                // 409, not the 500 the unique index produced when this path tried to insert a second
                // row with an address USER.EMAIL already holds.
                .isEqualTo(ApplicationErrorCode.ALREADY_EXISTS);

        // The victim's row is never written, so the request cannot repaint their name or avatar with
        // values it supplied, and no identity is attached to them.
        verify(userRepository, never()).save(any(User.class));
        verify(identityPort, never()).save(any());
    }

    @Test
    void unverifiedEmailOnAFreeAddressStillCreatesAnAccount() {
        // Unverified is not a rejection of the person. Nothing holds this address, so there is
        // nothing to take over and no unique index to violate.
        when(identityPort.findByProvider("google", "newcomer-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newcomer@corp.test")).thenReturn(Optional.empty());
        when(allocator.deriveBaseUsername(anyString(), anyString(), anyString())).thenReturn("newcomer");
        when(allocator.allocateUniqueUsername(anyString(), anyString())).thenReturn("newcomer");
        when(userRepository.save(any(User.class)))
                .thenAnswer(call -> ((User) call.getArgument(0)).withId(99L));

        User result = service.loginOrSignUp(new UserLoginOrSignUpCommand(
                "google", "newcomer-sub", "newcomer@corp.test", false, "Newcomer", null));

        assertThat(result.getId()).isEqualTo(99L).isNotEqualTo(existing.getId());
        assertThat(result.getUsername()).isEqualTo("newcomer");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
    }
}
