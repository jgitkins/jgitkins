package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.presentation.mapper.BranchRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.application.port.in.BranchDeleteUseCase;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.BranchCreateRequest;
import io.jgitkins.server.presentation.util.LocationUriBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repositories/{repositoryId}/branches")
@Tag(name = "Branch Management", description = "브랜치 조회/생성/삭제")
public class BranchController {

    private final BranchLoadUseCase branchLoadUseCase;
    private final BranchCreateUseCase branchCreateUseCase;
    private final BranchDeleteUseCase branchDeleteUseCase;
    private final BranchRequestMapper branchRequestMapper;

    @Operation(summary = "Create branch")
    @PostMapping
    public ResponseEntity<Void> create(@PathVariable Long repositoryId,
                                       @RequestBody BranchCreateRequest request) {

        BranchCreateCommand createCommand = branchRequestMapper.toCommand(repositoryId, request);
        branchCreateUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ResponseEntity.created(location).build();

    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(@PathVariable Long repositoryId) {
        return ApiResponse.ok(branchLoadUseCase.getBranches(repositoryId));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(@PathVariable Long repositoryId, @PathVariable String branchName) {
        return ApiResponse.ok(branchLoadUseCase.getBranch(repositoryId, branchName));
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long repositoryId,
                                                          @PathVariable String branchName) {

        branchDeleteUseCase.deleteBranch(repositoryId, branchName);
        return ApiResponse.noContent();
    }
}
