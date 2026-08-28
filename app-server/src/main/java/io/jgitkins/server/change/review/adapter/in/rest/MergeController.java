package io.jgitkins.server.change.review.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.adapter.in.support.ReviewRequesterResolver;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Merge", description = "병합관리")
public class MergeController {

    private final MergeabilityCheckUseCase mergeabilityCheckUseCase;
    private final MergeUseCase mergeUseCase;
    private final ReviewRequesterResolver reviewRequesterResolver;

    private Long requireRequester(String subject) {
        return reviewRequesterResolver.resolve(subject)
                .orElseThrow(() -> new UnauthenticatedException("Authentication required"));
    }

    @Operation(summary = "Check Mergeability", description = "소스 브랜치가 타겟 브랜치로 병합 가능한지 확인")
    @GetMapping("/repositories/{namespace}/{repoName}/merge/check")
    public ResponseEntity<ApiResponse<MergeResult>> checkMergeability(
            @PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @RequestParam String sourceBranch,
            @RequestParam String targetBranch
    ) throws IOException {
        MergeResult result = mergeabilityCheckUseCase.checkMergeability(namespace, repoName, sourceBranch, targetBranch);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "Merge", description = "소스 브랜치를 타겟 브랜치로 병합")
    @PostMapping("/repositories/{namespace}/{repoName}/merge")
    public ResponseEntity<ApiResponse<MergeResult>> performMerge(
            @PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @Valid @RequestBody MergeRequest request,
            @AuthenticationPrincipal(expression = "username") String subject
    ) throws IOException {
        // The requester comes from the principal, never from the body. MergeRequest is bound from
        // the request payload; an actor field there would be caller-controlled.
        MergeResult result = mergeUseCase.performMerge(
                namespace, repoName, request, requireRequester(subject));
        return ApiResponse.ok(result);
    }
}
