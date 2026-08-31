package io.jgitkins.server.repository.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.RepositoryMemberAddRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repository Members")
@RequestMapping("/api/repositories/{repositoryId}/members")
public class RepositoryMemberController {

    private final RepositoryMemberManagementUseCase repositoryMemberManagementUseCase;
    private final RepositoryMemberLoadUseCase repositoryMemberLoadUseCase;


    /**
     * Resolves the caller once, before any use case is touched.
     *
     * <p>A malformed principal must not reach the application layer: if it did, the first observable
     * effect of a broken credential would be a database read for whatever id was salvaged from it.
     */


    /**
     * The requester, or 401.
     *
     * <p>Rejected here rather than inside the use case: the first observable effect of an absent or
     * unusable credential must not be a database read for whatever id was salvaged from it.
     */
    private static Long requireRequester(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            throw new UnauthenticatedException("Authentication required");
        }
        return currentUser.userId();
    }

    @Operation(summary = "Add repository member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable @Positive Long repositoryId,
                                                       @Valid @RequestBody RepositoryMemberAddRequest request,
                                                       @CurrentUser AuthenticatedUser currentUser) {
        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(
                requireRequester(currentUser), repositoryId, request.userId(), request.role());
        repositoryMemberManagementUseCase.addRepositoryMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove repository member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable @Positive Long repositoryId,
                                                          @PathVariable @Positive Long userId,
                                                          @CurrentUser AuthenticatedUser currentUser) {
        repositoryMemberManagementUseCase.removeRepositoryMember(
                requireRequester(currentUser), repositoryId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List repository members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<RepositoryMemberSummary>>> listMembers(
            @PathVariable @Positive Long repositoryId,
            @CurrentUser AuthenticatedUser currentUser) {
        // requireRequester, not optionalRequester: a member list is never public, so an absent
        // caller is a rejection rather than a narrower result.
        return ApiResponse.ok(repositoryMemberLoadUseCase.getRepositoryMembers(
                requireRequester(currentUser), repositoryId));
    }
}
