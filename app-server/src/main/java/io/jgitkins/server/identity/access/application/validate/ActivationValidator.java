package io.jgitkins.server.identity.access.application.validate;

import io.jgitkins.server.identity.access.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.identity.access.application.exception.UsernameAlreadyExistsException;
import io.jgitkins.server.identity.access.application.port.out.OrganizationNameUniquenessPort;
import io.jgitkins.server.identity.access.application.port.out.OwnedRepositoryCountPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.Username;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivationValidator {
    private final UserRepository userRepository;
    private final OrganizationNameUniquenessPort organizationNameUniquenessPort;
    private final OwnedRepositoryCountPort ownedRepositoryCountPort;

    public Username validateUsername(String username) {
        return Username.from(username);
    }

    public void validateUsernameNotTaken(Username requested, Long userId) {
        userRepository.findByUsername(requested.getValue())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new UsernameAlreadyExistsException("Username already exists"); });
    }

    public void validateOrganizeNameNotTakenIfCompatible(Username requested) {
        if (!organizationNameUniquenessPort.isAvailableForUsername(requested.getValue())) {
            throw new OrganizeAlreadyExistsException();
        }
    }

    public void validateUserHasNoRepositories(Long userId) {
        if (ownedRepositoryCountPort.countByUserId(userId) > 0) {
            throw new ApplicationException(ApplicationErrorCode.UNPROCESSABLE,
                    "Cannot rename user with existing repositories");
        }
    }
}
