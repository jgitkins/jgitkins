package io.jgitkins.server.identity.access.application.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.internal.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    @Test
    void loginOrSignUp_throwsWhenProviderIdentityMissing() {
        UserService service = new UserService(mock(UserRepository.class), mock(UserIdentityPersistencePort.class),
                mock(UsernameAllocator.class), new UserProfileUpdater());

        UserLoginOrSignUpCommand command = new UserLoginOrSignUpCommand(null, "sub", null, false, null, null);

        assertThrows(IllegalArgumentException.class, () ->
                service.loginOrSignUp(command));
    }

    @Test
    void loginOrSignUp_signsInExistingIdentity() {
        UserRepository userRepository = mock(UserRepository.class);
        UserIdentityPersistencePort identityPort = mock(UserIdentityPersistencePort.class);
        UsernameAllocator allocator = mock(UsernameAllocator.class);
        UserProfileUpdater updater = new UserProfileUpdater();

        User user = User.createWithStatus("user", "a@b.com", "User", null, UserStatus.ACTIVE).withId(1L);
        UserIdentity identity = UserIdentity.rehydrate(10L, 1L, "google", "sub", "a@b.com", true, "User", null,
                user.getCreatedAt(), user.getUpdatedAt());

        when(identityPort.findByProvider("google", "sub")).thenReturn(Optional.of(identity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepository, identityPort, allocator, updater);

        UserLoginOrSignUpCommand command = new UserLoginOrSignUpCommand("google", "sub", "a@b.com", true, "User", null);

        User result = service.loginOrSignUp(command);

        assertEquals(1L, result.getId());
        verify(identityPort, never()).save(any(UserIdentity.class));
    }

    @Test
    void loginOrSignUp_signsUpWhenIdentityMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserIdentityPersistencePort identityPort = mock(UserIdentityPersistencePort.class);
        UsernameAllocator allocator = mock(UsernameAllocator.class);
        UserProfileUpdater updater = new UserProfileUpdater();

        when(identityPort.findByProvider("google", "sub")).thenReturn(Optional.empty());
        when(allocator.deriveBaseUsername(any(), any(), any())).thenReturn("base");
        when(allocator.allocateUniqueUsername("base", "sub")).thenReturn("unique");
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user.withId(2L);
        });

        UserService service = new UserService(userRepository, identityPort, allocator, updater);

        UserLoginOrSignUpCommand command = new UserLoginOrSignUpCommand("google", "sub", "a@b.com", true, "User", null);

        User result = service.loginOrSignUp(command);

        assertEquals(2L, result.getId());
        verify(identityPort).save(any(UserIdentity.class));
    }
}
