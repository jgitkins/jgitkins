package io.jgitkins.server.collaboration.adapter.in.rest;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.collaboration.application.port.out.UserIdentityPort;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeMemberRequestMapper;
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
    private final OrganizeMemberRequestMapper organizeMemberRequestMapper;
    private final UserIdentityPort userIdentityPort;

    @Operation(summary = "Add organize member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long organizeId,
                                                       @RequestBody OrganizeMemberAddRequest request) {
        Long requesterUserId = userIdentityPort.resolveCurrentActiveUserId()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED,
                        "Authentication required"));
        OrganizeMemberAddCommand command = organizeMemberRequestMapper.toCommand(organizeId, request, requesterUserId);
        organizeMemberAddUseCase.addOrganizeMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove organize member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long organizeId,
                                                          @PathVariable Long userId) {
        Long requesterUserId = userIdentityPort.resolveCurrentActiveUserId()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED,
                        "Authentication required"));
        organizeMemberRemoveUseCase.removeOrganizeMember(organizeId, requesterUserId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List organize members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<OrganizeMemberSummary>>> listMembers(@PathVariable Long organizeId) {
        return ApiResponse.ok(organizeMemberQueryUseCase.getOrganizeMembers(organizeId));
    }
}
