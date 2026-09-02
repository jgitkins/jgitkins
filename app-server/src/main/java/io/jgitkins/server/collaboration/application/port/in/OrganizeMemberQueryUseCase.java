package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.OrganizeMemberSummary;
import java.util.List;

public interface OrganizeMemberQueryUseCase {
    List<OrganizeMemberSummary> getOrganizeMembers(Long organizeId);
}
