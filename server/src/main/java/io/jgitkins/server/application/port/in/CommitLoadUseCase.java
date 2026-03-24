package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.CommitHistory;

import java.util.List;

public interface CommitLoadUseCase {
    CommitHistory getCommit(String namespace, String repoName, String commitHash);
    List<CommitHistory> getCommits(String namespace, String repoName, String branch);
}
