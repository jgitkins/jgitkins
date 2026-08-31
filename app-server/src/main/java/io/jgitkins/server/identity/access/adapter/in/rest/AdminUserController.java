package io.jgitkins.server.identity.access.adapter.in.rest;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.AdminUserUpdateUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.UserStatusUpdateRequest;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import io.jgitkins.server.shared.application.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator-only user operations.
 *
 * <p>Every method here took no principal before 2026-08-28, so the whole controller was reachable
 * unauthenticated: SecurityConfig is {@code anyRequest().permitAll()} and this application uses no
 * method security, which left the status endpoint able to set any account to BLOCKED or DELETED
 * from an unauthenticated request. The subject is now resolved and passed down; the ADMIN check
 * itself lives in the service so a second inbound adapter cannot bypass it.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Users")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserQueryUseCase adminUserQueryUseCase;
    private final AdminUserUpdateUseCase adminUserUpdateUseCase;




    @Operation(summary = "List users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAdminSummary>>> listUsers(
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(adminUserQueryUseCase.getUsers(AuthenticatedUser.requireUserId(currentUser)));
    }

    @Operation(summary = "Get user detail")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAdminDetail>> getUser(
            @PathVariable @Positive Long userId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ApiResponse.ok(adminUserQueryUseCase.getUser(AuthenticatedUser.requireUserId(currentUser), userId));
    }

    @Operation(summary = "Update user status")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        adminUserUpdateUseCase.updateUserStatus(AuthenticatedUser.requireUserId(currentUser), userId, request.status());
        return ApiResponse.ok();
    }
}
