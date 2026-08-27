package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

	/**
	 * @param requesterUserId the authenticated caller, or {@code null} for an anonymous request.
	 *	 Nullable on the read side and not on the write side, deliberately: a public repository is
	 *	 readable without a caller, and forcing a value here would either reject anonymous reads or
	 *	 invent a sentinel actor that the visibility filter would then have to recognise.
	 */
	RepositoryOverviewResult getOverview(Long requesterUserId, Long repositoryId, String branch);

	RepositoryOverviewResult getOverviewByPath(Long requesterUserId, String namespace, String repoName,
			String branch);
}
