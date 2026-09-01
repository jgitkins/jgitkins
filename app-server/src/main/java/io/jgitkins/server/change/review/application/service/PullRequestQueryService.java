package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.contract.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.exception.PullRequestNotFoundException;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestDetailMapper;
import io.jgitkins.server.change.review.application.port.in.GetPullRequestDetailUseCase;
import io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.application.support.PullRequestMergeabilityResolver;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PullRequestQueryService implements GetPullRequestDetailUseCase {
    private final PullRequestRepository pullRequestRepository;
    private final RepositoryReferencePort repositoryReferencePort;
    private final RepositoryReadAccessPort repositoryReadAccessPort;
    private final PullRequestMergeabilityResolver mergeabilityResolver;
    private final PullRequestDetailMapper detailMapper;

    @Override @Transactional(readOnly = true)
    public PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId, Long requesterUserId)
            throws IOException {
        PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new PullRequestNotFoundException(pullRequestId));
        ReviewRepositoryReference repository = repositoryReferencePort.findById(pullRequest.getRepositoryId())
                .orElseThrow(() -> new RepositoryReferenceNotFoundException(pullRequest.getRepositoryId().getValue()));
        // Authorize before the mergeability work, which reads branch heads off disk. Both denials
        // here answer 404 -- "no such pull request" and "cannot see that repository" are the same
        // answer from outside, which is what stops the id from being an existence oracle.
        repositoryReadAccessPort.requireReadAccess(
                repository.namespace(), repository.repoName(), requesterUserId);
        BranchHeadSnapshot currentSource = mergeabilityResolver.currentSourceHead(repository, pullRequest);
        BranchHeadSnapshot currentTarget = mergeabilityResolver.currentTargetHead(repository, pullRequest);
        PullRequest observed = pullRequest.markTargetDrifted(currentTarget);
        MergeabilityAssessment assessment = mergeabilityResolver.assess(repository, observed);
        return detailMapper.toDetail(observed, currentSource, currentTarget, assessment);
    }
}
