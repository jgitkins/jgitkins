package io.jgitkins.server.application.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserIdentity;
import io.jgitkins.server.domain.model.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    @Test
    void loginOrSignUp_throwsWhenProviderIdentityMissing() {
        UserService service = new UserService(mock(UserPersistencePort.class), mock(UserIdentityPersistencePort.class),
                mock(UsernameAllocator.class), new UserProfileUpdater());

        UserLoginOrSignUpCommand command = UserLoginOrSignUpCommand.builder()
                .providerSub("sub")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                service.loginOrSignUp(command));
    }

    @Test
    void loginOrSignUp_signsInExistingIdentity() {
        UserPersistencePort userPort = mock(UserPersistencePort.class);
        UserIdentityPersistencePort identityPort = mock(UserIdentityPersistencePort.class);
        UsernameAllocator allocator = mock(UsernameAllocator.class);
        UserProfileUpdater updater = new UserProfileUpdater();

        User user = User.createWithStatus("user", "a@b.com", "User", null, UserStatus.ACTIVE).withId(1L);
        UserIdentity identity = UserIdentity.rehydrate(10L, 1L, "google", "sub", "a@b.com", true, "User", null,
                user.getCreatedAt(), user.getUpdatedAt());

        when(identityPort.findByProvider("google", "sub")).thenReturn(Optional.of(identity));
        when(userPort.findById(1L)).thenReturn(Optional.of(user));
        when(userPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userPort, identityPort, allocator, updater);

        UserLoginOrSignUpCommand command = UserLoginOrSignUpCommand.builder()
                .providerName("google")
                .providerSub("sub")
                .email("a@b.com")
                .emailVerified(true)
                .name("User")
                .build();

        User result = service.loginOrSignUp(command);

        assertEquals(1L, result.getId());
        verify(identityPort, never()).save(any(UserIdentity.class));
    }

    @Test
    void loginOrSignUp_signsUpWhenIdentityMissing() {
        UserPersistencePort userPort = mock(UserPersistencePort.class);
        UserIdentityPersistencePort identityPort = mock(UserIdentityPersistencePort.class);
        UsernameAllocator allocator = mock(UsernameAllocator.class);
        UserProfileUpdater updater = new UserProfileUpdater();

        when(identityPort.findByProvider("google", "sub")).thenReturn(Optional.empty());
        when(allocator.deriveBaseUsername(any(), any(), any())).thenReturn("base");
        when(allocator.allocateUniqueUsername("base", "sub")).thenReturn("unique");
        when(userPort.findByEmail(any())).thenReturn(Optional.empty());
        when(userPort.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user.withId(2L);
        });

        UserService service = new UserService(userPort, identityPort, allocator, updater);

        UserLoginOrSignUpCommand command = UserLoginOrSignUpCommand.builder()
                .providerName("google")
                .providerSub("sub")
                .email("a@b.com")
                .emailVerified(true)
                .name("User")
                .build();

        User result = service.loginOrSignUp(command);

        assertEquals(2L, result.getId());
        verify(identityPort).save(any(UserIdentity.class));
    }
}
