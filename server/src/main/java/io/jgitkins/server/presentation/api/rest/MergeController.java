package io.jgitkins.server.presentation.api.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.jgitkins.server.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.application.port.in.MergeUseCase;
import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.presentation.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Merge", description = "병합관리")
public class MergeController {

    private final MergeabilityCheckUseCase mergeabilityCheckUseCase;
    private final MergeUseCase mergeUseCase;

    @Operation(summary = "Check Mergeability", description = "소스 브랜치가 타겟 브랜치로 병합 가능한지 확인")
    @GetMapping("/repositories/{namespace}/{repoName}/merge/check")
    public ResponseEntity<ApiResponse<MergeResult>> checkMergeability(
            @PathVariable String namespace,
            @PathVariable String repoName,
            @RequestParam String sourceBranch,
            @RequestParam String targetBranch
    ) throws IOException {
        MergeResult result = mergeabilityCheckUseCase.checkMergeability(namespace, repoName, sourceBranch, targetBranch);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "Merge", description = "소스 브랜치를 타겟 브랜치로 병합")
    @PostMapping("/repositories/{namespace}/{repoName}/merge")
    public ResponseEntity<ApiResponse<MergeResult>> performMerge(
            @PathVariable String namespace,
            @PathVariable String repoName,
            @RequestBody MergeRequest request
    ) throws IOException {
        MergeResult result = mergeUseCase.performMerge(namespace, repoName, request);
        return ApiResponse.ok(result);
    }

}
