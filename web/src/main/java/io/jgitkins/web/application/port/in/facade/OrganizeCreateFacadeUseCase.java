package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.OrganizeCreateRequest;
import io.jgitkins.web.application.dto.OrganizeCreateResult;

public interface OrganizeCreateFacadeUseCase {

    OrganizeCreateResult createOrganize(OrganizeCreateRequest request);
}
