package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.OrganizeMemberSummary;
import java.util.List;

public interface OrganizeMemberQueryUseCase {
    List<OrganizeMemberSummary> getOrganizeMembers(Long organizeId);
}
