package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestResultMapper;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PullRequestCreateService implements CreatePullRequestUseCase {
    private final PullRequestRepository pullRequestRepository;
    private final RepositoryReferencePort repositoryReferencePort;
    private final BranchHeadPort branchHeadPort;
    private final RepositoryWriteAccessPort repositoryWriteAccessPort;
    private final PullRequestResultMapper resultMapper;

    @Override @Transactional
    public PullRequestResult createPullRequest(PullRequestCreateCommand command, Long requesterUserId) {
        // Before the repository lookup: an unauthenticated caller must not learn from the error
        // whether a namespace/name pair resolves. requireWriteAccess answers 401 with no requester,
        // 404 for a repository they cannot see, and 403 for one they can see but may not write.
        repositoryWriteAccessPort.requireWriteAccess(
                command.namespace(), command.repoName(), requesterUserId);
        ReviewRepositoryReference repository = repositoryReferencePort.findByPath(command.namespace(), command.repoName())
                .orElseThrow(() -> new RepositoryReferenceNotFoundException(command.namespace(), command.repoName()));
        BranchHeadSnapshot source = branchHeadPort.getCurrentHead(repository, command.sourceBranch());
        BranchHeadSnapshot target = branchHeadPort.getCurrentHead(repository, command.targetBranch());
        return resultMapper.toResult(pullRequestRepository.save(PullRequest.create(repository.id(), source, target)));
    }
}
