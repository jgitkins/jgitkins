package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.application.port.in.OAuthLoginUseCase;
import io.jgitkins.server.presentation.common.ApiResponse;
import io.jgitkins.server.presentation.dto.OAuthLoginRequest;
import io.jgitkins.server.presentation.mapper.OAuthRequestMapper;
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
    public ResponseEntity<ApiResponse<OAuthLoginResult>> login(@RequestBody OAuthLoginRequest request) {
        OAuthLoginCommand command = oauthRequestMapper.toCommand(request);
        OAuthLoginResult result = oauthLoginUseCase.login(command);
        return ApiResponse.ok(result);
    }
}
