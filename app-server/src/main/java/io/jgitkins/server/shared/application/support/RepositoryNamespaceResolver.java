package io.jgitkins.server.shared.application.support;

import io.jgitkins.server.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryNamespaceResolver {

    private final OrganizePersistencePort organizePort;
    private final UserPersistencePort userPort;

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
}
