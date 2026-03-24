package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import io.jgitkins.server.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberRemoveUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.RepositoryMemberAddRequest;
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

    private final RepositoryMemberAddUseCase repositoryMemberAddUseCase;
    private final RepositoryMemberQueryUseCase repositoryMemberQueryUseCase;
    private final RepositoryMemberRemoveUseCase repositoryMemberRemoveUseCase;

    @Operation(summary = "Add repository member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long repositoryId,
                                                       @RequestBody RepositoryMemberAddRequest request) {
        RepositoryMemberAddCommand command = RepositoryMemberAddCommand.builder()
                .repositoryId(repositoryId)
                .userId(request.userId())
                .role(request.role())
                .build();
        repositoryMemberAddUseCase.addRepositoryMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove repository member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long repositoryId,
                                                          @PathVariable Long userId) {
        repositoryMemberRemoveUseCase.removeRepositoryMember(repositoryId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List repository members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<RepositoryMemberSummary>>> listMembers(@PathVariable Long repositoryId) {
        return ApiResponse.ok(repositoryMemberQueryUseCase.getRepositoryMembers(repositoryId));
    }
}
