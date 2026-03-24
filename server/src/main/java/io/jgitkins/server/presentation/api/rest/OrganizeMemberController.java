package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.OrganizeMemberAddRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Organize Members")
@RequestMapping("/api/organizes/{organizeId}/members")
public class OrganizeMemberController {

    private final OrganizeMemberAddUseCase organizeMemberAddUseCase;
    private final OrganizeMemberQueryUseCase organizeMemberQueryUseCase;
    private final OrganizeMemberRemoveUseCase organizeMemberRemoveUseCase;

    @Operation(summary = "Add organize member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long organizeId,
                                                       @RequestBody OrganizeMemberAddRequest request) {
        OrganizeMemberAddCommand command = OrganizeMemberAddCommand.builder()
                .organizeId(organizeId)
                .userId(request.userId())
                .role(request.role())
                .build();
        organizeMemberAddUseCase.addOrganizeMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove organize member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long organizeId,
                                                          @PathVariable Long userId) {
        organizeMemberRemoveUseCase.removeOrganizeMember(organizeId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List organize members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<OrganizeMemberSummary>>> listMembers(@PathVariable Long organizeId) {
        return ApiResponse.ok(organizeMemberQueryUseCase.getOrganizeMembers(organizeId));
    }
}
