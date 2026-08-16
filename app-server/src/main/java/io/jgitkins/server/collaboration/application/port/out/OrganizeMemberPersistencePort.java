package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.util.Optional;

public interface OrganizeMemberPersistencePort {

    OrganizeMember save(OrganizeMember member);

    boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId);

    Optional<OrganizeMember> findByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId);

    void deleteByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId);

    java.util.List<OrganizeMember> findAllByOrganizeId(OrganizeId organizeId);
}
