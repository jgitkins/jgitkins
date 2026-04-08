package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class RepositoryNotFoundException extends ApplicationException {

    public RepositoryNotFoundException() {
        super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND, "Repository not found");
    }

    public RepositoryNotFoundException(Long repositoryId) {
        super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND, "Repository not found: " + repositoryId);
    }

    public RepositoryNotFoundException(String namespace, String repoName) {
        super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND,
                String.format("Repository not found: %s/%s", namespace, repoName));
    }

    public RepositoryNotFoundException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND, message);
    }
}
