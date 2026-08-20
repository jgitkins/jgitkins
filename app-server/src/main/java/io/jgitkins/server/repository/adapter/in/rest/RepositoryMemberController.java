package io.jgitkins.server.repository.adapter.in.rest;

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

    @Operation(summary = "Add repository member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long repositoryId,
                                                       @RequestBody RepositoryMemberAddRequest request) {
        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(repositoryId, request.userId(), request.role());
        repositoryMemberManagementUseCase.addRepositoryMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove repository member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long repositoryId,
                                                          @PathVariable Long userId) {
        repositoryMemberManagementUseCase.removeRepositoryMember(repositoryId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List repository members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<RepositoryMemberSummary>>> listMembers(@PathVariable Long repositoryId) {
        return ApiResponse.ok(repositoryMemberLoadUseCase.getRepositoryMembers(repositoryId));
    }
}
