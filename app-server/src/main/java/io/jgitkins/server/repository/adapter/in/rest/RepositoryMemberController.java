package io.jgitkins.server.repository.adapter.in.rest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
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
@Tag(name = "Repository Members")
@RequestMapping("/api/repositories/{repositoryId}/members")
public class RepositoryMemberController {

    private final RepositoryMemberManagementUseCase repositoryMemberManagementUseCase;
    private final RepositoryMemberLoadUseCase repositoryMemberLoadUseCase;
    private final RequesterUserIdResolver requesterUserIdResolver;

    RepositoryMemberController(RepositoryMemberManagementUseCase repositoryMemberManagementUseCase,
                              RepositoryMemberLoadUseCase repositoryMemberLoadUseCase,
                              @Qualifier("identityRequesterUserIdResolver")
                              RequesterUserIdResolver requesterUserIdResolver) {
        this.repositoryMemberManagementUseCase = repositoryMemberManagementUseCase;
        this.repositoryMemberLoadUseCase = repositoryMemberLoadUseCase;
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


    @Operation(summary = "Add repository member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long repositoryId,
                                                       @RequestBody RepositoryMemberAddRequest request,
                                                       @AuthenticationPrincipal(expression = "username")
                                                       String subject) {
        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(
                requireRequester(subject), repositoryId, request.userId(), request.role());
        repositoryMemberManagementUseCase.addRepositoryMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove repository member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long repositoryId,
                                                          @PathVariable Long userId,
                                                          @AuthenticationPrincipal(expression = "username")
                                                          String subject) {
        repositoryMemberManagementUseCase.removeRepositoryMember(
                requireRequester(subject), repositoryId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List repository members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<RepositoryMemberSummary>>> listMembers(
            @PathVariable Long repositoryId,
            @AuthenticationPrincipal(expression = "username") String subject) {
        // requireRequester, not optionalRequester: a member list is never public, so an absent
        // caller is a rejection rather than a narrower result.
        return ApiResponse.ok(repositoryMemberLoadUseCase.getRepositoryMembers(
                requireRequester(subject), repositoryId));
    }
}
