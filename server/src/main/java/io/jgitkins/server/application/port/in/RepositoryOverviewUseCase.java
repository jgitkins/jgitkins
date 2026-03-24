package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import java.io.IOException;

public interface RepositoryOverviewUseCase {

	RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
