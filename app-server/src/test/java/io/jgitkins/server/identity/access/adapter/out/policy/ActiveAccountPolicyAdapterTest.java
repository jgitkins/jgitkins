package io.jgitkins.server.identity.access.adapter.out.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveAccountPolicyAdapterTest {
    @Mock CurrentUserPort currentUserPort;
    @Mock UserRepository userRepository;

    @Test void unauthenticatedUsesAuth001() {
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.empty());
        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> new ActiveAccountPolicyAdapter(currentUserPort, userRepository).requireActiveUserId());
        assertEquals(ApplicationProblemSpec.UNAUTHENTICATED.getCode(), ex.getProblemSpec().getCode());
        assertEquals("Unauthenticated", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test void missingUserUsesUser404() {
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> new ActiveAccountPolicyAdapter(currentUserPort, userRepository).requireActiveUserId());
        assertEquals("USER-404", ex.getProblemSpec().getCode());
    }

    @Test void onlyActiveIsAllowed() {
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        assertEquals(7L, new ActiveAccountPolicyAdapter(currentUserPort, userRepository).requireActiveUserId());
    }

    @Test void inactiveStatusesAreDenied() {
        for (UserStatus status : new UserStatus[]{UserStatus.PENDING, UserStatus.BLOCKED, UserStatus.DELETED}) {
            when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
            when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user(status)));
            ApplicationException ex = assertThrows(ApplicationException.class,
                    () -> new ActiveAccountPolicyAdapter(currentUserPort, userRepository).requireActiveUserId());
            assertEquals("AUTH-403", ex.getProblemSpec().getCode());
            assertEquals("Access denied", ex.getMessage());
        }
    }

    private User user(UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return User.rehydrate(7L, "user", "user@example.test", "User", null, UserAuthority.USER,
                status, now, now, now);
    }
}
