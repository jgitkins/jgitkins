package io.jgitkins.web.application.service;

import io.jgitkins.web.application.dto.OrganizeFetchResult;
import io.jgitkins.web.application.dto.RepositoryCreateRequest;
import io.jgitkins.web.application.dto.RepositoryCreateResult;
import io.jgitkins.web.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import io.jgitkins.web.application.port.out.RepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RepositoryCreateService implements RepositoryCreateUseCase {

	private final OrganizePort organizePort;
	private final RepositoryPort repositoryPort;

	@Override
	public OrganizeFetchResult loadOwnerOptions() {
		return organizePort.fetchOrganizes();
	}

	@Override
	public RepositoryCreateResult createRepository(RepositoryCreateRequest request) {
		return repositoryPort.createRepository(request);
	}
}
