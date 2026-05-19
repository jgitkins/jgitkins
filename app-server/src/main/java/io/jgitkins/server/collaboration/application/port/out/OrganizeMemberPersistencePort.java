package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.util.Optional;

public interface OrganizeMemberPersistencePort {

    OrganizeMember save(OrganizeMember member);

    boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId);

    Optional<OrganizeMember> findByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId);

    void deleteByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId);

    java.util.List<OrganizeMember> findAllByOrganizeId(OrganizeId organizeId);
}
