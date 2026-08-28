package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityEvaluationUseCase;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeabilityEvaluationUseCase, MergeUseCase {
    private final MergePort mergePort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;
    private final RepositoryWriteAccessPort repositoryWriteAccessPort;
    @Override public MergeResult checkMergeability(String n, String r, String s, String t) throws IOException { return mergePort.previewMergeability(n, r, s, t); }
    @Override public MergeabilityAssessment evaluateMergeability(String n, String r, String s, String t) throws IOException { return mergeabilityAssessmentAssembler.toAssessment(mergePort.previewMergeability(n, r, s, t)); }
    /**
     * Merging moves the target branch, so it requires write access to the repository. Before this the
     * method was a single delegation to the port with no check of any kind, and the controller passed
     * no principal, so any anonymous caller could merge any branch of any repository including a
     * private one.
     */
    @Override
    public MergeResult performMerge(String n, String r, MergeRequest request, Long requesterUserId)
            throws IOException {
        repositoryWriteAccessPort.requireWriteAccess(n, r, requesterUserId);
        return mergePort.merge(n, r, request);
    }
}
