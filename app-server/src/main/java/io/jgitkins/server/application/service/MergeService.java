package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.in.MergeabilityEvaluationUseCase;
import io.jgitkins.server.application.port.in.MergeUseCase;
import io.jgitkins.server.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.application.port.out.MergeGitPort;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("legacyMergeService")
@RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeabilityEvaluationUseCase, MergeUseCase {

    private final MergeGitPort mergeGitPort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;

    @Override
    public MergeResult checkMergeability(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException {
        return mergeGitPort.previewMergeability(namespace, repoName, sourceBranch, targetBranch);
    }

    @Override
    public MergeabilityAssessment evaluateMergeability(String namespace, String repoName, String sourceBranch, String targetBranch)
            throws IOException {
        MergeResult result = mergeGitPort.previewMergeability(namespace, repoName, sourceBranch, targetBranch);
        return mergeabilityAssessmentAssembler.toAssessment(result);
    }

    @Override
    public MergeResult performMerge(String namespace, String repoName, MergeRequest request) throws IOException {
        return mergeGitPort.merge(namespace, repoName, request);
    }
}
