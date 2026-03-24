package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.result.UserSummary;
import io.jgitkins.server.application.port.in.PublicUserQueryUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users")
@RequestMapping("/api/users")
public class UserController {

    private final PublicUserQueryUseCase publicUserQueryUseCase;

    @Operation(summary = "List public users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSummary>>> listUsers() {
        return ApiResponse.ok(publicUserQueryUseCase.getUsers());
    }
}
