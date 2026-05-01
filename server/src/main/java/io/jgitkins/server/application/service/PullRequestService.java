package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.application.dto.result.PullRequestResult;
import io.jgitkins.server.application.exception.PullRequestNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.application.port.in.GetPullRequestDetailUseCase;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.pr.PullRequestDetailMapper;
import io.jgitkins.server.application.support.pr.PullRequestMergeabilityResolver;
import io.jgitkins.server.application.support.pr.PullRequestResultMapper;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import io.jgitkins.server.domain.pr.repository.PullRequestRepository;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PullRequestService implements CreatePullRequestUseCase, GetPullRequestDetailUseCase {

    private final PullRequestRepository pullRequestRepository;
    private final RepositoryPersistencePort repositoryPersistencePort;
    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchGitPort branchGitPort;
    private final PullRequestMergeabilityResolver mergeabilityResolver;
    private final PullRequestResultMapper resultMapper;
    private final PullRequestDetailMapper detailMapper;

    @Override
    @Transactional
    public PullRequestResult createPullRequest(PullRequestCreateCommand command) throws IOException {
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

    @Override
    @Transactional(readOnly = true)
    public PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId) throws IOException {
        PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new PullRequestNotFoundException(pullRequestId));
        Repository repository = repositoryPersistencePort.findById(pullRequest.getRepositoryId())
                .orElseThrow(() -> new RepositoryNotFoundException(pullRequest.getRepositoryId().getValue()));

        BranchHeadSnapshot currentSource = mergeabilityResolver.currentSourceHead(repository, pullRequest);
        BranchHeadSnapshot currentTarget = mergeabilityResolver.currentTargetHead(repository, pullRequest);
        PullRequest observed = pullRequest.markTargetDrifted(currentTarget);
        MergeabilityAssessment assessment = mergeabilityResolver.assess(repository, observed);

        return detailMapper.toDetail(observed, currentSource, currentTarget, assessment);
    }

    private BranchHeadSnapshot currentHead(String namespace, String repoName, String branchName) throws IOException {
        String commitHash = branchGitPort.getHeadCommitHash(namespace, repoName, branchName);
        return BranchHeadSnapshot.of(branchName, commitHash);
    }
}
