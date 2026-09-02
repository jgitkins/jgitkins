package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.OrganizeCreationResult;

import java.util.List;

public interface OrganizeLoadUseCase {
    OrganizeCreationResult getOrganize(Long organizeId);
    List<OrganizeCreationResult> getOrganizes();
    List<OrganizeCreationResult> getAccessibleOrganizes(Long requesterUserId);

}
