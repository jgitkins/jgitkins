package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.RepositoryDetailData;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import java.util.List;

public interface RepositoryDetailUseCase {

	RepositoryDetailData loadRepositoryDetail(Long repositoryId, String branch);

	RepositoryDetailData loadRepositoryByPath(String namespace, String repoName, String branch, String directory);

	List<RepositoryFileIndexEntry> loadRepositoryFileIndexByPath(String namespace, String repoName, String branch);

	List<RepositoryFileIndexEntry> searchRepositoryFilesByPath(String namespace, String repoName, String branch, String query, int limit);
}
