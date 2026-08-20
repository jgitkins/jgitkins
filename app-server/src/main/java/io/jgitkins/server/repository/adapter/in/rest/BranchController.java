package io.jgitkins.server.repository.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.core.web.api.uri.LocationUriBuilder;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.BranchCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.mapper.BranchRequestMapper;
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
    private final BranchManagementUseCase branchManagementUseCase;
    private final BranchRequestMapper branchRequestMapper;

    @Operation(summary = "Create branch")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@PathVariable Long repositoryId,
                                                    @RequestBody BranchCreateRequest request) {

        BranchCreateCommand createCommand = branchRequestMapper.toCommand(repositoryId, request);
        branchManagementUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ApiResponse.created(location);

    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(@PathVariable Long repositoryId) {
        return ApiResponse.ok(branchLoadUseCase.loadBranches(repositoryId));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(@PathVariable Long repositoryId, @PathVariable String branchName) {
        return ApiResponse.ok(branchLoadUseCase.loadBranch(repositoryId, branchName));
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long repositoryId,
                                                          @PathVariable String branchName) {

        branchManagementUseCase.deleteBranch(repositoryId, branchName);
        return ApiResponse.noContent();
    }
}
