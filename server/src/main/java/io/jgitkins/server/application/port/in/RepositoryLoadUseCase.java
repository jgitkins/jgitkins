package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryResult;

import java.util.List;

public interface RepositoryLoadUseCase {
    RepositoryResult getRepository(Long repositoryId);
    RepositoryResult getRepositoryByPath(String namespace, String repoName);
    List<RepositoryResult> getRepositories();

    List<RepositoryResult> getRepositoriesByUsername(String username);
}
