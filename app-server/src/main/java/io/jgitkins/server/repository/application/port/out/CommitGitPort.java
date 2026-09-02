package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.application.contract.external.CommitFile;
import io.jgitkins.server.repository.application.contract.CommitHistory;

import java.util.List;

public interface CommitGitPort {
    CommitHistory loadCommit(String namespace, String repoName, String commitHash);
    List<CommitHistory> listCommitHistory(String namespace, String repoName, String branch);
    void commit(String namespace,
                String repoName,
                String branch,
                String message,
                String authorName,
                String authorEmail,
                List<CommitFile> files);

}