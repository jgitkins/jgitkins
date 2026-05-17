package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.OrganizeFetchResult;
import io.jgitkins.web.application.dto.RepositoryCreateRequest;
import io.jgitkins.web.application.dto.RepositoryCreateResult;

public interface RepositoryCreateUseCase {

	OrganizeFetchResult loadOwnerOptions();

	RepositoryCreateResult createRepository(RepositoryCreateRequest request);
}
