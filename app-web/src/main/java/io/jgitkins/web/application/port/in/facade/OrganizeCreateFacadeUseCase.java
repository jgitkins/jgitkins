package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.OrganizeCreateRequest;
import io.jgitkins.web.application.contract.OrganizeCreateResult;

public interface OrganizeCreateFacadeUseCase {

    OrganizeCreateResult createOrganize(OrganizeCreateRequest request);
}
