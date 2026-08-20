package io.jgitkins.server.change.review.adapter.out.acl;

import io.jgitkins.server.change.review.application.exception.BranchHeadNotFoundException;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchHeadAclAdapter implements BranchHeadPort {
    private final BranchGitPort branchGitPort;
    @Override public BranchHeadSnapshot getCurrentHead(ReviewRepositoryReference repository, String branchName) {
        try {
            return BranchHeadSnapshot.of(branchName, branchGitPort.getHeadCommitHash(repository.namespace(), repository.repoName(), branchName));
        } catch (GitBranchRefMissingException e) {
            throw new BranchHeadNotFoundException(e.getBranchName());
        }
    }
}
