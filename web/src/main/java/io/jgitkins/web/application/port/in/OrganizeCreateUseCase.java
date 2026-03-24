package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.OrganizeCreateRequest;
import io.jgitkins.web.application.dto.OrganizeCreateResult;

public interface OrganizeCreateUseCase {

	OrganizeCreateResult createOrganize(OrganizeCreateRequest request);
}
