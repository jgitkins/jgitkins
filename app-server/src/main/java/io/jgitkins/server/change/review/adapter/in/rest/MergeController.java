package io.jgitkins.server.change.review.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.change.review.application.contract.MergeRequest;
import io.jgitkins.server.change.review.application.contract.MergeResult;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
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



    @Operation(summary = "Check Mergeability", description = "소스 브랜치가 타겟 브랜치로 병합 가능한지 확인")
    @GetMapping("/repositories/{namespace}/{repoName}/merge/check")
    public ResponseEntity<ApiResponse<MergeResult>> checkMergeability(
            @PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @RequestParam String sourceBranch,
            @RequestParam String targetBranch,
            @CurrentUser AuthenticatedUser currentUser
    ) throws IOException {
        // Nullable, unlike performMerge below: previewing a merge reads the repository, and a public
        // repository is readable anonymously. requireUserId here would answer 401 to a logged-out
        // visitor looking at a public repository.
        MergeResult result = mergeabilityCheckUseCase.checkMergeability(namespace, repoName,
                sourceBranch, targetBranch, AuthenticatedUser.userIdOrNull(currentUser));
        return ApiResponse.ok(result);
    }

    @Operation(summary = "Merge", description = "소스 브랜치를 타겟 브랜치로 병합")
    @PostMapping("/repositories/{namespace}/{repoName}/merge")
    public ResponseEntity<ApiResponse<MergeResult>> performMerge(
            @PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @Valid @RequestBody MergeRequest request,
            @CurrentUser AuthenticatedUser currentUser
    ) throws IOException {
        // The requester comes from the principal, never from the body. MergeRequest is bound from
        // the request payload; an actor field there would be caller-controlled.
        MergeResult result = mergeUseCase.performMerge(
                namespace, repoName, request, AuthenticatedUser.requireUserId(currentUser));
        return ApiResponse.ok(result);
    }
}
