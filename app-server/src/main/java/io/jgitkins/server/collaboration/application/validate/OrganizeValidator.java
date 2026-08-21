package io.jgitkins.server.collaboration.application.validate;

import io.jgitkins.server.collaboration.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeValidator {

    private final OrganizeRepository organizeRepository;
    private final OrganizeMembershipQueryPort organizeMemberPort;

    public void validateCreation(OrganizeName name) {
        if (organizeRepository.findByName(name).isPresent()) {
            throw new OrganizeAlreadyExistsException("Organize name already exists: " + name.getValue());
        }
    }

    public Organize findByIdOrThrow(Long organizeId) {
        return organizeRepository.findById(OrganizeId.of(organizeId))
                .orElseThrow(() -> new OrganizeNotFoundException(organizeId));
    }

    public boolean isAccessible(Organize organize, Long currentUserId) {
        if (organize == null || currentUserId == null || organize.getId() == null) {
            return false;
        }
        boolean isOwner = organize.getOwnerId() != null
                && currentUserId.equals(organize.getOwnerId().getValue());
        return isOwner || organizeMemberPort.findRoleByOrganizeIdAndUserId(
                organize.getId().getValue(), currentUserId).isPresent();
    }
}
