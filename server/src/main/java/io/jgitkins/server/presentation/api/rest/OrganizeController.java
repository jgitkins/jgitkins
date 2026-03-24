package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.OrganizeCreationRequest;
import io.jgitkins.server.presentation.mapper.OrganizeRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Create Organize")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> createOrganize(@RequestBody OrganizeCreationRequest request) {
        OrganizeCreationCommand command = organizeRequestMapper.toCommand(request);
        OrganizeCreationResult result = organizeCreationUseCase.createOrganize(command);
        return ApiResponse.created(result.getId(), result);
    }

    @Operation(summary = "List Organizes")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getOrganizes() {
        return ApiResponse.ok(organizeLoadUseCase.getOrganizes());
    }

    @Operation(summary = "List Accessible Organizes")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OrganizeCreationResult>>> getAccessibleOrganizes() {
        return ApiResponse.ok(organizeLoadUseCase.getAccessibleOrganizes());
    }

    @Operation(summary = "Get Organize")
    @GetMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> getOrganize(@PathVariable Long organizeId) {
        return ApiResponse.ok(organizeLoadUseCase.getOrganize(organizeId));
    }

    @Operation(summary = "Delete Organize")
    @DeleteMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganize(@PathVariable Long organizeId) {
        organizeDeletionUseCase.deleteOrganize(organizeId);
        return ApiResponse.noContent();
    }
}
