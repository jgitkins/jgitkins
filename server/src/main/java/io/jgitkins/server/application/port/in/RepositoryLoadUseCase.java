package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryResult;

import java.util.List;

public interface RepositoryLoadUseCase {
    RepositoryResult loadRepository(Long repositoryId);
    RepositoryResult loadRepositoryByPath(String namespace, String repoName);
    List<RepositoryResult> loadRepositories();

    List<RepositoryResult> loadUserRepositories(String username);
}
