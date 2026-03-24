package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.NamespaceSummary;

public interface NamespaceFacadeUseCase {

    NamespaceSummary getNamespaceSummary(String namespace);

}
