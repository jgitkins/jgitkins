package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.OrganizeCreationResult;

import java.util.List;

public interface OrganizeLoadUseCase {
    OrganizeCreationResult getOrganize(Long organizeId);
    List<OrganizeCreationResult> getOrganizes();
    List<OrganizeCreationResult> getAccessibleOrganizes();

}
