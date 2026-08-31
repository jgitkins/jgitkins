package io.jgitkins.server.repository.adapter.in.rest;

import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.repository.application.port.in.CommitLoadUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Commit", description = "커밋 관리")
@RequestMapping("/repositories")
public class RepositoryCommitController {

    private final CommitLoadUseCase commitLoadUseCase;

    @Operation(summary = "View Commit Detail", description = "커밋 상세 조회")
    @GetMapping("/{namespace}/{repoName}/commits/{commitHash}")
    public ResponseEntity<ApiResponse<CommitHistory>> getCommitDetail(@PathVariable String namespace,
                                                                      @PathVariable String repoName,
                                                                      @PathVariable String commitHash,
                                                                      @CurrentUser AuthenticatedUser currentUser) {
        // Nullable requester: public repositories stay readable anonymously. Until task P0a this
        // route passed the namespace and name straight to git with no requester and no rule, so the
        // commit contents of any private repository were readable by anyone who knew its name.
        CommitHistory commitHistory = commitLoadUseCase.getCommit(namespace, repoName, commitHash,
                AuthenticatedUser.userIdOrNull(currentUser));
        return ApiResponse.ok(commitHistory);
    }

    @Operation(summary = "View Commit Histories", description = "커밋 이력 조회")
    @GetMapping("/{namespace}/{repoName}/branches/{branch}/commits")
    public ResponseEntity<ApiResponse<List<CommitHistory>>> getBranchCommitHistories(@PathVariable String namespace,
                                                                                     @PathVariable String repoName,
                                                                                     @PathVariable String branch,
                                                                                     @CurrentUser AuthenticatedUser currentUser) {
        List<CommitHistory> commitHistories = commitLoadUseCase.getCommits(namespace, repoName, branch,
                AuthenticatedUser.userIdOrNull(currentUser));
        return ApiResponse.ok(commitHistories);
    }
}
