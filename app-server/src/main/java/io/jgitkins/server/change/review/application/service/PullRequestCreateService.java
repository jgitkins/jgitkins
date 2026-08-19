package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestResultMapper;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
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
    private final PullRequestResultMapper resultMapper;

    @Override @Transactional
    public PullRequestResult createPullRequest(PullRequestCreateCommand command) {
        ReviewRepositoryReference repository = repositoryReferencePort.findByPath(command.namespace(), command.repoName())
                .orElseThrow(() -> new RepositoryReferenceNotFoundException(command.namespace(), command.repoName()));
        BranchHeadSnapshot source = branchHeadPort.getCurrentHead(repository, command.sourceBranch());
        BranchHeadSnapshot target = branchHeadPort.getCurrentHead(repository, command.targetBranch());
        return resultMapper.toResult(pullRequestRepository.save(PullRequest.create(repository.id(), source, target)));
    }
}
