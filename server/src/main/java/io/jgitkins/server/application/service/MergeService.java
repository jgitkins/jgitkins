package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.in.MergeUseCase;
import io.jgitkins.server.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.application.port.out.MergeGitPort;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MergeService implements MergeabilityCheckUseCase, MergeUseCase {

    private final MergeGitPort mergeGitPort;

    @Override
    public MergeResult checkMergeability(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException {
        return mergeGitPort.previewMerge(namespace, repoName, sourceBranch, targetBranch);
    }

    @Override
    public MergeResult performMerge(String namespace, String repoName, MergeRequest request) throws IOException {
        return mergeGitPort.merge(namespace, repoName, request);
    }
}
