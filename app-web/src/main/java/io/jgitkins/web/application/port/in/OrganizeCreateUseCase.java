package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.OrganizeCreateRequest;
import io.jgitkins.web.application.contract.OrganizeCreateResult;

public interface OrganizeCreateUseCase {

	OrganizeCreateResult createOrganize(OrganizeCreateRequest request);
}
