package io.jgitkins.server.application.service;

import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.error.DomainErrorCode;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.application.validate.ActivationValidator;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserStatus;
import io.jgitkins.server.domain.model.vo.Username;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private CurrentUserPort currentUserPersistencePort;

    @Mock
    private UserPersistencePort userPort;

    @Mock
    private ActivationValidator validator;

    @InjectMocks
    private UserProfileService service;

    @Test
    void activate_activatesAndSavesUser() {
        Username requested = Username.from("new_name");
        when(validator.validateUsername("new_name")).thenReturn(requested);
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        User pending = User.createWithStatus("temp", "a@b.com", "User", null, UserStatus.PENDING).withId(1L);
        when(userPort.findById(1L)).thenReturn(Optional.of(pending));
        when(userPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.activate("new_name");

        verify(validator).validateUsernameNotTaken(requested, 1L);
        verify(validator).validateOrganizeNameNotTakenIfCompatible(requested);
        verify(validator).validateUserHasNoRepositories(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userPort).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("new_name", saved.getUsername());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void activate_translatesAlreadyActivatedDomainException() {
        Username requested = Username.from("new_name");
        when(validator.validateUsername("new_name")).thenReturn(requested);
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        User activeUser = User.createWithStatus("active_user", "a@b.com", "User", null, UserStatus.ACTIVE).withId(1L);
        when(userPort.findById(1L)).thenReturn(Optional.of(activeUser));

        JgitkinsException exception = assertThrows(JgitkinsException.class, () -> service.activate("new_name"));

        assertSame(DomainErrorCode.USER_ALREADY_ACTIVATED, exception.getErrorCode());
        verify(userPort, never()).save(any(User.class));
    }
}
