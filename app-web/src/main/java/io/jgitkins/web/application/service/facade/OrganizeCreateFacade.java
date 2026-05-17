package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.dto.OrganizeCreateRequest;
import io.jgitkins.web.application.dto.OrganizeCreateResult;
import io.jgitkins.web.application.port.in.OrganizeCreateUseCase;
import io.jgitkins.web.application.port.in.facade.OrganizeCreateFacadeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizeCreateFacade implements OrganizeCreateFacadeUseCase {

    private final OrganizeCreateUseCase organizeCreateUseCase;

    @Override
    public OrganizeCreateResult createOrganize(OrganizeCreateRequest request) {
        return organizeCreateUseCase.createOrganize(request);
    }
}
