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
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;

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


    /**
     * The requester, or 401.
     *
     * <p>Rejected here rather than inside the use case: the first observable effect of an absent or
     * unusable credential must not be a database read for whatever id was salvaged from it.
     */
    private static Long requireRequester(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            // This wording, not the "Authentication required" the other controllers use. The message
            // reaches the response body, so it is wire contract; standardising it here would be a
            // behaviour change smuggled inside a refactor, and a refactor that also moves the wire is
            // one nobody can revert with confidence.
            throw new UnauthenticatedException("An authenticated user is required");
        }
        return currentUser.userId();
    }

    @Operation(summary = "Create Organize")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> createOrganize(
            @Valid @RequestBody OrganizeCreationRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        Long requesterUserId = requireRequester(currentUser);
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
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(organizeLoadUseCase.getAccessibleOrganizes(
                AuthenticatedUser.userIdOrNull(currentUser)));
    }

    @Operation(summary = "Get Organize")
    @GetMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<OrganizeCreationResult>> getOrganize(@PathVariable @Positive Long organizeId) {
        return ApiResponse.ok(organizeLoadUseCase.getOrganize(organizeId));
    }

    @Operation(summary = "Delete Organize")
    @DeleteMapping("/{organizeId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganize(
            @PathVariable @Positive Long organizeId,
            @CurrentUser AuthenticatedUser currentUser) {
        // 401, not 403, for a caller with no credentials. Task 2.91 settled that for this
        // controller: the truth is "I do not know who you are", not "you are not allowed".
        Long requesterUserId = requireRequester(currentUser);
        organizeDeletionUseCase.deleteOrganize(requesterUserId, organizeId);
        return ApiResponse.noContent();
    }
}
