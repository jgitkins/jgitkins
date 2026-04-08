package io.jgitkins.server.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jgitkins.server.application.exception.UnauthenticatedException;
import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.port.in.SignupUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.application.validate.ActivationValidator;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.vo.Username;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService implements SignupUseCase {

    private final CurrentUserPort currentUserPersistencePort;
    private final UserPersistencePort userPort;
    private final ActivationValidator activationValidator;

    @Override
    @Transactional
    public void activate(String username) {
        Username requested = activationValidator.validateUsername(username);
        Long userId = currentUserId();
        User user = loadUser(userId);

        activationValidator.validateUsernameNotTaken(requested, userId);
        activationValidator.validateOrganizeNameNotTakenIfCompatible(requested);
        activationValidator.validateUserHasNoRepositories(userId);

        // DomainException(UserAlreadyActivatedException)은 재포장 없이 그대로 전파
        User updated = user.activateWithUsername(requested);
        userPort.save(updated);
    }

    private Long currentUserId() {
        return currentUserPersistencePort.resolveCurrentUserId()
                .orElseThrow(UnauthenticatedException::new);
    }

    private User loadUser(Long userId) {
        return userPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
