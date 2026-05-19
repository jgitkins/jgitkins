package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeValidator {

    private final OrganizePersistencePort organizePort;
    private final OrganizeMemberPersistencePort organizeMemberPort;

    public void validateCreation(OrganizeName name) {
        if (organizePort.findByName(name).isPresent()) {
            throw new OrganizeAlreadyExistsException("Organize name already exists: " + name.getValue());
        }
    }

    public Organize findByIdOrThrow(Long organizeId) {
        return organizePort.findById(OrganizeId.of(organizeId))
                .orElseThrow(() -> new OrganizeNotFoundException(organizeId));
    }

    public boolean isAccessible(Organize organize, UserId userId) {
        return userId.equals(organize.getOwnerId()) ||
                organizeMemberPort.existsByOrganizeIdAndUserId(organize.getId(), userId);
    }
}
