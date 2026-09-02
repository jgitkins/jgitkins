package io.jgitkins.server.collaboration.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import io.jgitkins.server.collaboration.application.contract.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.contract.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.collaboration.adapter.in.rest.contract.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.translator.OrganizeMemberRequestMapper;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
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
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;

@RestController
@RequiredArgsConstructor
@Tag(name = "Organize Members")
@RequestMapping("/api/organizes/{organizeId}/members")
public class OrganizeMemberManagementController {

    private final OrganizeMemberAddUseCase organizeMemberAddUseCase;
    private final OrganizeMemberQueryUseCase organizeMemberQueryUseCase;
    private final OrganizeMemberRemoveUseCase organizeMemberRemoveUseCase;
    private final OrganizeMemberRequestMapper organizeMemberRequestMapper;


    @Operation(summary = "Add organize member")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable @Positive Long organizeId,
                                                       @Valid @RequestBody OrganizeMemberAddRequest request,
                                                       @CurrentUser AuthenticatedUser currentUser) {
        Long requesterUserId = AuthenticatedUser.requireUserId(currentUser);
        OrganizeMemberAddCommand command = organizeMemberRequestMapper.toCommand(organizeId, request, requesterUserId);
        organizeMemberAddUseCase.addOrganizeMember(command);
        return ApiResponse.ok();
    }

    @Operation(summary = "Remove organize member")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable @Positive Long organizeId,
                                                          @PathVariable @Positive Long userId,
                                                          @CurrentUser AuthenticatedUser currentUser) {
        Long requesterUserId = AuthenticatedUser.requireUserId(currentUser);
        organizeMemberRemoveUseCase.removeOrganizeMember(organizeId, requesterUserId, userId);
        return ApiResponse.noContent();
    }

    @Operation(summary = "List organize members")
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<OrganizeMemberSummary>>> listMembers(@PathVariable @Positive Long organizeId) {
        return ApiResponse.ok(organizeMemberQueryUseCase.getOrganizeMembers(organizeId));
    }
}
