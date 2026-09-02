package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.contract.MergeRequest;
import io.jgitkins.server.change.review.application.contract.MergeResult;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityEvaluationUseCase;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort;
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
    private final RepositoryReadAccessPort repositoryReadAccessPort;
    /**
     * A merge preview is a diff of two branches, so seeing it is seeing the repository. Read access,
     * not write: previewing does not move anything.
     */
    @Override
    public MergeResult checkMergeability(String n, String r, String s, String t, Long requesterUserId)
            throws IOException {
        repositoryReadAccessPort.requireReadAccess(n, r, requesterUserId);
        return mergePort.previewMergeability(n, r, s, t);
    }
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
