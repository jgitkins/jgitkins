package io.jgitkins.server.identity.access.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.application.validate.ActivationValidator;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.vo.Username;
import lombok.RequiredArgsConstructor;

/**
 * Activation, with the actor supplied rather than discovered.
 *
 * <p>Task 2.63 removed this service's {@code CurrentUserPort} dependency. It previously asked the
 * security context who the caller was, in the middle of deciding what that caller may do. Two
 * consequences went with that: the service could not be exercised for a chosen actor without standing up
 * a security context, and a reader could not tell from the signature whose account was being activated.
 *
 * <p>The port is unchanged and still used by its other callers — {@code InProcessUserIdentityAdapter},
 * {@code RepositoryActorAclAdapter}, {@code ActiveAccountPolicyAdapter} and
 * {@code CurrentUserSecurityAdapter}. This task narrowed one caller, it did not retire the port.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService implements SignupUseCase {

    private final UserRepository userRepository;
    private final ActivationValidator activationValidator;

    @Override
    @Transactional
    public void activate(Long requesterUserId, String username) {
        // Guarded here as well as at the adapter. The adapter is the only current caller, but a use case
        // that trusts its own arguments to be non-null is one refactor away from activating an
        // arbitrary account, and the cost of the check is a branch.
        if (requesterUserId == null) {
            throw new UnauthenticatedException();
        }
        Username requested = activationValidator.validateUsername(username);
        User user = loadUser(requesterUserId);

        activationValidator.validateUsernameNotTaken(requested, requesterUserId);
        activationValidator.validateOrganizeNameNotTakenIfCompatible(requested);
        activationValidator.validateUserHasNoRepositories(requesterUserId);

        // DomainException(UserAlreadyActivatedException)은 재포장 없이 그대로 전파
        User updated = user.activateWithUsername(requested);
        userRepository.save(updated);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
