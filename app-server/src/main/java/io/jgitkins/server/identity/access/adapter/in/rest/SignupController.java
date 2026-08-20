package io.jgitkins.server.identity.access.adapter.in.rest;

import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.UserUsernameUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Signup")
@RequestMapping("/api/signup")
public class SignupController {

    private final SignupUseCase signupUseCase;

    @Operation(summary = "Activate signup with username")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody UserUsernameUpdateRequest request) {
        signupUseCase.activate(request.username());
        return ApiResponse.ok();
    }
}
