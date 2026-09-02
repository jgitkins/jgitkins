package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.NamespaceSummary;

public interface NamespaceFacadeUseCase {

    NamespaceSummary getNamespaceSummary(String namespace);

}
