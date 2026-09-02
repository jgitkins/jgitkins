package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.CommitHistory;
import io.jgitkins.server.repository.application.port.in.CommitLoadUseCase;
import io.jgitkins.server.repository.application.exception.CommitNotFoundException;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommitService implements CommitLoadUseCase {

    private final CommitGitPort commitGitPort;
    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public CommitHistory getCommit(String namespace,
                                   String repoName,
                                   String commitHash,
                                   Long requesterUserId) {
        // Before the port, not after. The git port resolves a path from the namespace and name and
        // reads objects off disk; letting it run first would mean the denial happens after the data
        // was already loaded, and an error that differs by whether the repository exists is itself
        // an oracle.
        repositoryAccessValidator.validateReadAccess(namespace, repoName, requesterUserId);
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
                                          String branch,
                                          Long requesterUserId) {
        repositoryAccessValidator.validateReadAccess(namespace, repoName, requesterUserId);
        return commitGitPort.listCommitHistory(namespace, repoName, branch);
    }
}
