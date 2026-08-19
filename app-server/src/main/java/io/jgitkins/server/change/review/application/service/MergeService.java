package io.jgitkins.server.change.review.application.service;

import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityEvaluationUseCase;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeabilityEvaluationUseCase, MergeUseCase {
    private final MergePort mergePort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;
    @Override public MergeResult checkMergeability(String n, String r, String s, String t) throws IOException { return mergePort.previewMergeability(n, r, s, t); }
    @Override public MergeabilityAssessment evaluateMergeability(String n, String r, String s, String t) throws IOException { return mergeabilityAssessmentAssembler.toAssessment(mergePort.previewMergeability(n, r, s, t)); }
    @Override public MergeResult performMerge(String n, String r, MergeRequest request) throws IOException { return mergePort.merge(n, r, request); }
}
