package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.application.mapper.PullRequestResultMapper;
import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import io.jgitkins.server.domain.pr.repository.PullRequestRepository;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PullRequestCreateService implements CreatePullRequestUseCase {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchGitPort branchGitPort;
    private final PullRequestResultMapper resultMapper;

    @Override
    @Transactional
    public PullRequestResult createPullRequest(PullRequestCreateCommand command) {
        Repository repository = repositoryLookupService.resolveByPath(command.namespace(), command.repoName())
                .orElseThrow(() -> new RepositoryNotFoundException(command.namespace(), command.repoName()));
        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getPath().getValue();
        BranchHeadSnapshot source = currentHead(namespace, repoName, command.sourceBranch());
        BranchHeadSnapshot target = currentHead(namespace, repoName, command.targetBranch());

        PullRequest pullRequest = PullRequest.create(repository.getId(), source, target);
        PullRequest saved = pullRequestRepository.save(pullRequest);
        return resultMapper.toResult(saved);
    }

    private BranchHeadSnapshot currentHead(String namespace, String repoName, String branchName) {
        try {
            String commitHash = branchGitPort.getHeadCommitHash(namespace, repoName, branchName);
            return BranchHeadSnapshot.of(branchName, commitHash);
        } catch (GitBranchRefMissingException e) {
            throw new BranchNotFoundException(e.getBranchName());
        }
    }
}
