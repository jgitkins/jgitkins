package io.jgitkins.server.repository.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
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
    private final RequesterUserIdResolver requesterUserIdResolver;


    /**
     * Resolves the caller once, before any use case is touched.
     *
     * <p>A malformed principal must not reach the application layer: if it did, the first observable
     * effect of a broken credential would be a database read for whatever id was salvaged from it.
     */
    private Long requireRequester(String subject) {
        return requesterUserIdResolver.resolve(subject)
                .orElseThrow(() -> new UnauthenticatedException("Authentication required"));
    }


    @Operation(summary = "Create branch")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@PathVariable @Positive Long repositoryId,
                                                    @Valid @RequestBody BranchCreateRequest request,
                                                    @AuthenticationPrincipal(expression = "username")
                                                    String subject) {

        BranchCreateCommand createCommand = branchRequestMapper.toCommand(
                requireRequester(subject), repositoryId, request);
        branchManagementUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ApiResponse.created(location);

    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(@PathVariable @Positive Long repositoryId) {
        return ApiResponse.ok(branchLoadUseCase.loadBranches(repositoryId));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(@PathVariable @Positive Long repositoryId, @PathVariable String branchName) {
        return ApiResponse.ok(branchLoadUseCase.loadBranch(repositoryId, branchName));
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable @Positive Long repositoryId,
                                                          @PathVariable String branchName,
                                                          @AuthenticationPrincipal(expression = "username")
                                                          String subject) {

        branchManagementUseCase.deleteBranch(requireRequester(subject), repositoryId, branchName);
        return ApiResponse.noContent();
    }
}
