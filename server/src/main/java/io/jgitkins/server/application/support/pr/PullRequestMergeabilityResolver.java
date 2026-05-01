package io.jgitkins.server.application.support.pr;

import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.application.port.out.MergeGitPort;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PullRequestMergeabilityResolver {

    private final BranchGitPort branchGitPort;
    private final MergeGitPort mergeGitPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;

    public BranchHeadSnapshot currentSourceHead(Repository repository, PullRequest pullRequest) throws IOException {
        return currentHead(repository, pullRequest.getSource().branchName().getValue());
    }

    public BranchHeadSnapshot currentTargetHead(Repository repository, PullRequest pullRequest) throws IOException {
        return currentHead(repository, pullRequest.getTarget().branchName().getValue());
    }

    public MergeabilityAssessment assess(Repository repository, PullRequest pullRequest) throws IOException {
        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getPath().getValue();
        MergeResult result = mergeGitPort.previewMergeability(
                namespace,
                repoName,
                pullRequest.getSource().branchName().getValue(),
                pullRequest.getTarget().branchName().getValue());
        return mergeabilityAssessmentAssembler.toAssessment(result);
    }

    private BranchHeadSnapshot currentHead(Repository repository, String branchName) throws IOException {
        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getPath().getValue();
        String commitHash = branchGitPort.getHeadCommitHash(namespace, repoName, branchName);
        return BranchHeadSnapshot.of(branchName, commitHash);
    }
}
