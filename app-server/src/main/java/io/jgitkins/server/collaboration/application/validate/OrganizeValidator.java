package io.jgitkins.server.collaboration.application.validate;

import io.jgitkins.server.collaboration.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeValidator {

    private final OrganizeRepository organizePort;
    private final OrganizeMembershipQueryPort organizeMemberPort;

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
        return organize.getOwnerId() != null
                && userId.getValue().equals(organize.getOwnerId().getValue()) ||
                organizeMemberPort.existsByOrganizeIdAndUserId(
                        organize.getId(),
                        io.jgitkins.server.collaboration.domain.vo.MemberUserId.of(userId.getValue()));
    }
}
