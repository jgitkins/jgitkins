package io.jgitkins.server.change.review.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.change.review.application.contract.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.contract.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.contract.result.PullRequestResult;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.change.review.application.port.in.GetPullRequestDetailUseCase;
import io.jgitkins.server.change.review.adapter.in.rest.dto.request.PullRequestCreateRequest;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pull Request", description = "변경검토")
@RequestMapping("/repositories/{namespace}/{repoName}/pull-requests")
public class PullRequestManagementController {

    private final CreatePullRequestUseCase createPullRequestUseCase;
    private final GetPullRequestDetailUseCase getPullRequestDetailUseCase;



    @Operation(summary = "Create Pull Request", description = "source 브랜치와 target 브랜치의 검토 요청을 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<PullRequestResult>> createPullRequest(
            @PathVariable @NotBlank String namespace,
            @PathVariable @NotBlank String repoName,
            @Valid @RequestBody PullRequestCreateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        // Required, not nullable: opening a pull request writes.
        Long requesterUserId = AuthenticatedUser.requireUserId(currentUser);
        PullRequestCreateCommand command = new PullRequestCreateCommand(
                namespace,
                repoName,
                request.sourceBranch(),
                request.targetBranch());
        PullRequestResult result = createPullRequestUseCase.createPullRequest(command, requesterUserId);
        return ApiResponse.created(result.getId(), result);
    }

    @Operation(summary = "Get Pull Request Detail", description = "Pull Request의 저장 snapshot과 현재 상태를 조회")
    @GetMapping("/{pullRequestId}")
    public ResponseEntity<ApiResponse<PullRequestDetailResult>> getPullRequestDetail(
            @PathVariable @Positive Long pullRequestId,
            @CurrentUser AuthenticatedUser currentUser) throws IOException {
        // Nullable: a public repository's pull request is readable anonymously.
        return ApiResponse.ok(getPullRequestDetailUseCase.getPullRequestDetail(
                PullRequestId.of(pullRequestId),
                AuthenticatedUser.userIdOrNull(currentUser)));
    }
}
