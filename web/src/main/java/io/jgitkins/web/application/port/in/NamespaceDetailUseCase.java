package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.NamespaceSummary;

public interface NamespaceDetailUseCase {

	NamespaceSummary loadNamespaceDetail(String namespace);
}
