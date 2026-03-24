package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.CommitHistory;
import io.jgitkins.server.application.port.in.CommitLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                                                                      @PathVariable String commitHash) {

        CommitHistory commitHistory = commitLoadUseCase.getCommit(namespace, repoName, commitHash);
        return ApiResponse.ok(commitHistory);
    }

    @Operation(summary = "View Commit Histories", description = "커밋 이력 조회")
    @GetMapping("/{namespace}/{repoName}/branches/{branch}/commits")
    public ResponseEntity<ApiResponse<List<CommitHistory>>> getBranchCommitHistories(@PathVariable String namespace,
                                                                                     @PathVariable String repoName,
                                                                                     @PathVariable String branch) {

        List<CommitHistory> commitHistories = commitLoadUseCase.getCommits(namespace, repoName, branch);
        return ApiResponse.ok(commitHistories);
    }
}
