package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.result.UserAdminDetail;
import io.jgitkins.server.application.dto.result.UserAdminSummary;
import io.jgitkins.server.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.application.port.in.AdminUserUpdateUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.UserStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Users")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserQueryUseCase adminUserQueryUseCase;
    private final AdminUserUpdateUseCase adminUserUpdateUseCase;

    @Operation(summary = "List users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAdminSummary>>> listUsers() {
        return ApiResponse.ok(adminUserQueryUseCase.getUsers());
    }

    @Operation(summary = "Get user detail")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAdminDetail>> getUser(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserQueryUseCase.getUser(userId));
    }

    @Operation(summary = "Update user status")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long userId,
                                                          @RequestBody UserStatusUpdateRequest request) {
        adminUserUpdateUseCase.updateUserStatus(userId, request.status());
        return ApiResponse.ok();
    }
}
