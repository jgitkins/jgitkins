package io.jgitkins.server.change.review.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

public final class RepositoryReferenceNotFoundException extends ApplicationException {
    public RepositoryReferenceNotFoundException(String namespace, String repoName) { super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND, "Repository not found: " + namespace + "/" + repoName); }
    public RepositoryReferenceNotFoundException(Long repositoryId) { super(ApplicationProblemSpec.REPOSITORY_NOT_FOUND, "Repository not found: " + repositoryId); }
}
