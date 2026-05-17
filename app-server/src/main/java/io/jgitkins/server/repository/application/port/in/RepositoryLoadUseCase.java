package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

import java.util.List;

public interface RepositoryLoadUseCase {
    RepositoryResult loadRepository(Long repositoryId);
    RepositoryResult loadRepositoryByPath(String namespace, String repoName);
    List<RepositoryResult> loadRepositories();

    List<RepositoryResult> loadUserRepositories(String username);
}
