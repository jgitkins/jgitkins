package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivationValidator {

    private final UserPersistencePort userPort;
    private final OrganizePersistencePort organizePort;
    private final RepositoryPersistencePort repositoryPort;

    public Username validateUsername(String username) {
        return Username.from(username);
    }

    public void validateUsernameNotTaken(Username requested, Long userId) {
        userPort.findByUsername(requested.getValue())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ApplicationException(
                            ApplicationErrorCode.USERNAME_ALREADY_EXISTS,
                            "Username already exists");
                });
    }

    public void validateOrganizeNameNotTakenIfCompatible(Username requested) {
        if (!requested.isOrganizeNameCompatible()) {
            return;
        }
        organizePort.findByName(OrganizeName.from(requested.getValue()))
                .ifPresent(existing -> {
                    throw new ApplicationException(
                            ApplicationErrorCode.ORGANIZE_ALREADY_EXISTS,
                            "Namespace already exists");
                });
    }

    public void validateUserHasNoRepositories(Long userId) {
        long count = repositoryPort.countByOwner(OwnerType.USER, OwnerId.of(userId));
        if (count > 0) {
            throw new ApplicationException(ApplicationErrorCode.ORGANIZE_ALREADY_EXISTS,
                    "Cannot rename user with existing repositories");
        }
    }
}
