package io.jgitkins.server.application.support;

import io.jgitkins.server.application.exception.InvalidNamespaceException;
import io.jgitkins.server.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryNamespaceResolver {

    private final OrganizePersistencePort organizePort;
    private final UserPersistencePort userPort;

    public NamespaceInfo resolve(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new InvalidNamespaceException(
                    "Namespace cannot be empty");
        }

        String target = namespace.trim();

        // 1. Try to resolve as organize
        NamespaceInfo organizeInfo = resolveAsOrganize(target);
        if (organizeInfo != null) {
            return organizeInfo;
        }

        // 2. Try to resolve as user
        NamespaceInfo userInfo = resolveAsUser(target);
        if (userInfo != null) {
            return userInfo;
        }

        throw new InvalidNamespaceException(
                "Could not resolve namespace to organize or user: " + target);
    }

    public String resolve(Repository repository) {
        return resolve(repository.getOwnerType(), repository.getOwnerId());
    }

    public String resolve(OwnerType ownerType, OwnerId ownerId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return organizePort.findById(io.jgitkins.server.domain.model.vo.OrganizeId.of(ownerId.getValue()))
                    .map(org -> org.getName().getValue())
                    .orElseThrow(OrganizeNotFoundException::new);
        } else {
            User user = userPort.findById(ownerId.getValue())
                    .orElseThrow(UserNotFoundException::new);
            return user.getUsername();
        }
    }

    private NamespaceInfo resolveAsOrganize(String namespace) {
        return organizePort.findByName(io.jgitkins.server.domain.model.vo.OrganizeName.from(namespace))
                .map(org -> new NamespaceInfo(OwnerType.ORGANIZATION, OwnerId.of(org.getId().getValue())))
                .orElse(null);
    }

    private NamespaceInfo resolveAsUser(String username) {
        return userPort.findUserIdByUsername(username)
                .map(userId -> new NamespaceInfo(OwnerType.USER, OwnerId.of(userId)))
                .orElse(null);
    }

    public record NamespaceInfo(OwnerType ownerType, OwnerId ownerId) {
    }
}
