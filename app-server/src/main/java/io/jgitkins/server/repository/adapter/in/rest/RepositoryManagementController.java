package io.jgitkins.server.repository.adapter.in.rest;

import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.RepositoryCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.mapper.RepositoryRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
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


    /**
     * Resolves the caller once, before any use case is touched.
     *
     * <p>A malformed principal must not reach the application layer: if it did, the first observable
     * effect of a broken credential would be a database read for whatever id was salvaged from it.
     */

    /**
     * Resolves the caller for a read, where anonymous is allowed.
     *
     * <p>Distinct from {@code AuthenticatedUser.requireUserId}: a public repository is readable without a caller, so an
     * absent principal returns null rather than a 401. A malformed one still throws — the resolver draws
     * that line, and a broken credential must not be silently downgraded to "anonymous", which would let
     * a corrupted token quietly read exactly the public subset instead of being reported.
     */
    private static Long optionalRequester(AuthenticatedUser currentUser) {
        return AuthenticatedUser.userIdOrNull(currentUser);
    }




    @Operation(summary = "Create Repository", description = "ownerType required.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResult>> create(
            @Valid @RequestBody RepositoryCreateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        Long requesterUserId = AuthenticatedUser.requireUserId(currentUser);
        RepositoryCreateCommand createCommand = repositoryRequestMapper.toCommand(requesterUserId, request);
        RepositoryResult result = repositoryManagementUseCase.create(createCommand);
        return ApiResponse.created(result.id(), result);
    }

    @Operation(summary = "Get Repository Metadata")
    @GetMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<RepositoryResult>> getRepository(
            @PathVariable @Positive Long repositoryId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepository(optionalRequester(currentUser), repositoryId));
    }

    @Operation(summary = "Get Repositories")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getRepositories(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(repositoryLoadUseCase.loadRepositories(optionalRequester(currentUser)));
    }

    @Operation(summary = "Get User Repositories by Username")
    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<List<RepositoryResult>>> getUserRepositories(
            @PathVariable("username") @NotBlank String username,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(
                repositoryLoadUseCase.loadUserRepositories(optionalRequester(currentUser), username));
    }

    @Operation(summary = "Delete Repository")
    @DeleteMapping("/{repositoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @PathVariable @Positive Long repositoryId,
            @CurrentUser AuthenticatedUser currentUser) {
        repositoryManagementUseCase.deleteRepository(AuthenticatedUser.requireUserId(currentUser), repositoryId);
        return ApiResponse.noContent();
    }

    /***
     * jgitkins-app-web
     */
    @Operation(summary = "Get Repository Overview")
    @GetMapping("/{repositoryId}/overview")
    public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getOverview(@PathVariable @Positive Long repositoryId,
                                                                             @RequestParam(name = "branch", required = false) String branch,
                                                                             @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(repositoryOverviewUseCase.getOverview(
                optionalRequester(currentUser), repositoryId, branch));
    }

}
