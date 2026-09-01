package io.jgitkins.server.shared.application.support;

import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.repository.domain.aggregate.Repository;

import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryNamespaceResolver {

    private final OrganizeQueryPort organizePort;
    private final UserQueryPort userQueryPort;

    public String resolve(Repository repository) {
        return resolve(repository.getOwnerType(), repository.getOwnerId());
    }

    public String resolve(OwnerType ownerType, RepositoryOwnerId ownerId) {
        if (ownerType == OwnerType.ORGANIZATION) {
            return organizePort.findById(io.jgitkins.server.collaboration.domain.vo.OrganizeId.of(ownerId.getValue()))
                    .map(org -> org.getName().getValue())
                    .orElseThrow(OrganizeNotFoundException::new);
        } else {
            return userQueryPort.findUsernameById(ownerId.getValue())
                    .orElseThrow(UserNotFoundException::new);
        }
    }
}
