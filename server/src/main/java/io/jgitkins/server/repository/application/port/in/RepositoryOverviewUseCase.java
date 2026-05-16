package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

	RepositoryOverviewResult getOverview(Long repositoryId, String branch);

	RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch);
}
