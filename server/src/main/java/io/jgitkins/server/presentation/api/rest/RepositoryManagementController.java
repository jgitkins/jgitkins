package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.RepositoryCreateRequest;
import io.jgitkins.server.presentation.mapper.RepositoryRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Management", description = "저장소 관리")
@RequestMapping("/api/repositories")
@Validated
public class RepositoryManagementController {

    private final RepositoryManagementUseCase repositoryManagementUseCase;
    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryOverviewUseCase repositoryOverviewUseCase;

    private final RepositoryRequestMapper repositoryRequestMapper;

    @Operation(summary = "Create Repository", description = "ownerType required.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResult>> create(@Valid @RequestBody RepositoryCreateRequest request) {
        RepositoryCreateCommand createCommand = repositoryRequestMapper.toCommand(request);
        RepositoryResult result = repositoryManagementUseCase.create(createCommand);
        return ApiResponse.created(result.id(), result);
    }

    @Operation(summary = "Get Repository Metadata")
    @GetMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<RepositoryResult>> getRepository(@PathVariable Long repositoryId) {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepository(repositoryId));
    }

    @Operation(summary = "Get Repositories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getRepositories() {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepositories());
    }

    @Operation(summary = "Get User Repositories by Username")
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getUserRepositories(@PathVariable("username") @NotBlank String username) {
        return ApiResponse.ok(repositoryLoadUseCase.loadUserRepositories(username));
    }

    @Operation(summary = "Delete Repository")
    @DeleteMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(@PathVariable Long repositoryId) {
        repositoryManagementUseCase.deleteRepository(repositoryId);
        return ApiResponse.noContent();
    }

    /***
     * jgitkins-web
     */
    @Operation(summary = "Get Repository Overview")
    @GetMapping("/{repositoryId}/overview")
    public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getOverview(@PathVariable Long repositoryId,
                                                                             @RequestParam(name = "branch", required = false) String branch) throws java.io.IOException {
        return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repositoryId, branch));
    }

}
