package io.jgitkins.web.application.service;

import io.jgitkins.web.application.dto.OrganizeCreateRequest;
import io.jgitkins.web.application.dto.OrganizeCreateResult;
import io.jgitkins.web.application.port.in.OrganizeCreateUseCase;
import io.jgitkins.web.application.port.out.OrganizePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizeCreateService implements OrganizeCreateUseCase {

	private final OrganizePort organizePort;

	@Override
	public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
		return organizePort.createOrganize(request);
	}
}
