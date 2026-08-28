package io.jgitkins.server.collaboration.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeRequestMapper;
import io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Organize Management")
@RequestMapping("/api/organizes")
public class OrganizeController {

    private final OrganizeCreationUseCase organizeCreationUseCase;
    private final OrganizeLoadUseCase organizeLoadUseCase;
    private final OrganizeDeletionUseCase organizeDeletionUseCase;

    private final OrganizeRequestMapper organizeRequestMapper;
    private final RequesterUserIdResolver requesterUserIdResolver;

    @Operation(summary = "Create Organize")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> createOrganize(
            @Valid @RequestBody OrganizeCreationRequest request,
            @AuthenticationPrincipal(expression = "username") String subject) {
        Long requesterUserId = requesterUserIdResolver.resolve(subject)
                .orElseThrow(() -> new OrganizeAccessDeniedException("An authenticated user is required"));
        OrganizeCreationCommand command = organizeRequestMapper.toCommand(request, requesterUserId);
        OrganizeCreationResult result = organizeCreationUseCase.createOrganize(command);
        return ApiResponse.created(result.id(), result);
    }

    @Operation(summary = "List Organizes")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getOrganizes() {
        return ApiResponse.ok(organizeLoadUseCase.getOrganizes());
    }

    @Operation(summary = "List Accessible Organizes")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getAccessibleOrganizes(
            @AuthenticationPrincipal(expression = "username") String subject) {
        return ApiResponse.ok(organizeLoadUseCase.getAccessibleOrganizes(
                requesterUserIdResolver.resolve(subject).orElse(null)));
    }

    @Operation(summary = "Get Organize")
    @GetMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> getOrganize(@PathVariable @Positive Long organizeId) {
        return ApiResponse.ok(organizeLoadUseCase.getOrganize(organizeId));
    }

    @Operation(summary = "Delete Organize")
    @DeleteMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganize(@PathVariable @Positive Long organizeId) {
        organizeDeletionUseCase.deleteOrganize(organizeId);
        return ApiResponse.noContent();
    }
}
