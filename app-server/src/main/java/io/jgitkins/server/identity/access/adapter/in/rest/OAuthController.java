package io.jgitkins.server.identity.access.adapter.in.rest;

import jakarta.validation.Valid;
import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.OAuthLoginRequest;
import io.jgitkins.server.identity.access.adapter.in.rest.mapper.OAuthRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "OAuth")
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthLoginUseCase oauthLoginUseCase;
    private final OAuthRequestMapper oauthRequestMapper;

    @Operation(summary = "Issue JWT token from OAuth login data")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OAuthLoginResult>> login(@Valid @RequestBody OAuthLoginRequest request) {
        OAuthLoginCommand command = oauthRequestMapper.toCommand(request);
        OAuthLoginResult result = oauthLoginUseCase.login(command);
        return ApiResponse.ok(result);
    }
}
