package io.jgitkins.server.application.port.out;

public interface RepositoryGitPort {
    void initialize(String namespace, String repoName);
    void deleteRepository(String namespace, String repoName);

    void updateHeadReference(String namespace, String repoName, String branch);
}
