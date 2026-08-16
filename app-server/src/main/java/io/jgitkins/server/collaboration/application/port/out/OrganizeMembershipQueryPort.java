package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;

public interface OrganizeMembershipQueryPort {

    boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId);
}
