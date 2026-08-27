package io.jgitkins.server.repository.adapter.in.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Tag(name = "Repository Management", description = "저장소 관리")
@RequestMapping("/api/repositories")
@Validated
public class RepositoryManagementController {

    private final RepositoryManagementUseCase repositoryManagementUseCase;
    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryOverviewUseCase repositoryOverviewUseCase;

    private final RepositoryRequestMapper repositoryRequestMapper;
    private final RequesterUserIdResolver requesterUserIdResolver;

    /**
     * Explicit constructor rather than {@code @RequiredArgsConstructor}: the qualifier must sit on the
     * constructor parameter. Two beans of type {@code RequesterUserIdResolver} exist with deliberately
     * different error semantics, and the wrong one turns a malformed principal into a silent empty.
     */
    RepositoryManagementController(RepositoryManagementUseCase repositoryManagementUseCase,
                                   RepositoryLoadUseCase repositoryLoadUseCase,
                                   RepositoryOverviewUseCase repositoryOverviewUseCase,
                                   RepositoryRequestMapper repositoryRequestMapper,
                                   @Qualifier("identityRequesterUserIdResolver")
                                   RequesterUserIdResolver requesterUserIdResolver) {
        this.repositoryManagementUseCase = repositoryManagementUseCase;
        this.repositoryLoadUseCase = repositoryLoadUseCase;
        this.repositoryOverviewUseCase = repositoryOverviewUseCase;
        this.repositoryRequestMapper = repositoryRequestMapper;
        this.requesterUserIdResolver = requesterUserIdResolver;
    }

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


    @Operation(summary = "Create Repository", description = "ownerType required.")
    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResult>> create(
            @Valid @RequestBody RepositoryCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String subject) {
        Long requesterUserId = requireRequester(subject);
        RepositoryCreateCommand createCommand = repositoryRequestMapper.toCommand(requesterUserId, request);
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
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal(expression = "username") String subject) {
        repositoryManagementUseCase.deleteRepository(requireRequester(subject), repositoryId);
        return ApiResponse.noContent();
    }

    /***
     * jgitkins-app-web
     */
    @Operation(summary = "Get Repository Overview")
    @GetMapping("/{repositoryId}/overview")
    public ResponseEntity<ApiResponse<RepositoryOverviewResult>> getOverview(@PathVariable Long repositoryId,
                                                                             @RequestParam(name = "branch", required = false) String branch) {
        return ApiResponse.ok(repositoryOverviewUseCase.getOverview(repositoryId, branch));
    }

}
