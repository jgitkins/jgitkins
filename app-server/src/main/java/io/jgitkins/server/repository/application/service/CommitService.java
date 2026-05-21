package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.repository.application.port.in.CommitLoadUseCase;
import io.jgitkins.server.repository.application.exception.CommitNotFoundException;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommitService implements CommitLoadUseCase {

    private final CommitGitPort commitGitPort;

    @Override
    @Transactional(readOnly = true)
    public CommitHistory getCommit(String namespace,
                                   String repoName,
                                   String commitHash) {
        try {
            return commitGitPort.loadCommit(namespace, repoName, commitHash);
        } catch (GitCommitObjectMissingException e) {
            throw new CommitNotFoundException(e.getCommitHash());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommitHistory> getCommits(String namespace,
                                          String repoName,
                                          String branch) {
        return commitGitPort.listCommitHistory(namespace, repoName, branch);
    }
}
