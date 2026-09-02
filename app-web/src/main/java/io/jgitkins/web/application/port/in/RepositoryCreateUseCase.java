package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.OrganizeFetchResult;
import io.jgitkins.web.application.contract.RepositoryCreateRequest;
import io.jgitkins.web.application.contract.RepositoryCreateResult;

public interface RepositoryCreateUseCase {

	OrganizeFetchResult loadOwnerOptions();

	RepositoryCreateResult createRepository(RepositoryCreateRequest request);
}
