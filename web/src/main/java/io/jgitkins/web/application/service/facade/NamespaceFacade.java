package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.dto.NamespaceSummary;
import io.jgitkins.web.application.port.in.NamespaceDetailUseCase;
import io.jgitkins.web.application.port.in.facade.NamespaceFacadeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NamespaceFacade implements NamespaceFacadeUseCase {

    private final NamespaceDetailUseCase namespaceDetailUseCase;

    @Override
    public NamespaceSummary getNamespaceSummary(String namespace) {
        return namespaceDetailUseCase.loadNamespaceDetail(namespace);
    }
}
