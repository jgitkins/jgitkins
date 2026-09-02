package io.jgitkins.server.repository.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.core.web.api.uri.LocationUriBuilder;
import io.jgitkins.server.repository.adapter.in.rest.contract.request.BranchCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.translator.BranchRequestMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repositories/{repositoryId}/branches")
@Tag(name = "Branch Management", description = "브랜치 조회/생성/삭제")
public class BranchManagementController {

    private final BranchLoadUseCase branchLoadUseCase;
    private final BranchManagementUseCase branchManagementUseCase;
    private final BranchRequestMapper branchRequestMapper;


    /**
     * Resolves the caller once, before any use case is touched.
     *
     * <p>A malformed principal must not reach the application layer: if it did, the first observable
     * effect of a broken credential would be a database read for whatever id was salvaged from it.
     */



    @Operation(summary = "Create branch")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@PathVariable @Positive Long repositoryId,
                                                    @Valid @RequestBody BranchCreateRequest request,
                                                    @CurrentUser AuthenticatedUser currentUser) {

        BranchCreateCommand createCommand = branchRequestMapper.toCommand(
                AuthenticatedUser.requireUserId(currentUser), repositoryId, request);
        branchManagementUseCase.createBranch(createCommand);

        URI location = LocationUriBuilder.create(request.branchName());
        return ApiResponse.created(location);

    }

    @Operation(summary = "Get Branches")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchSearchResult>>> getBranches(
            @PathVariable @Positive Long repositoryId,
            @CurrentUser AuthenticatedUser currentUser) {
        // Nullable requester: a public repository's branch list is readable anonymously, and the
        // visibility rule decides. Demanding a requester here would answer 401 to a logged-out
        // visitor browsing a public repository.
        return ApiResponse.ok(branchLoadUseCase.loadBranches(
                repositoryId, AuthenticatedUser.userIdOrNull(currentUser)));
    }

    @Operation(summary = "Get Branch")
    @GetMapping("/{branchName}")
    public ResponseEntity<ApiResponse<BranchSearchResult>> getBranch(
            @PathVariable @Positive Long repositoryId,
            @PathVariable String branchName,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(branchLoadUseCase.loadBranch(
                repositoryId, branchName, AuthenticatedUser.userIdOrNull(currentUser)));
    }

    @Operation(summary = "Delete branch")
    @DeleteMapping("/{branchName}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable @Positive Long repositoryId,
                                                          @PathVariable String branchName,
                                                          @CurrentUser AuthenticatedUser currentUser) {

        branchManagementUseCase.deleteBranch(AuthenticatedUser.requireUserId(currentUser), repositoryId, branchName);
        return ApiResponse.noContent();
    }
}
