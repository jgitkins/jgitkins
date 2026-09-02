package io.jgitkins.web.application.service.support;

import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.infrastructure.util.PathUtils;
import org.springframework.stereotype.Component;

@Component
public class RepositoryKeyResolver {

	public RepositoryKey resolve(RepositorySummary repository) {
		if (repository == null) {
			return null;
		}
		return PathUtils.resolveRepositoryKey(repository.clonePath(), repository.path());
	}
}
