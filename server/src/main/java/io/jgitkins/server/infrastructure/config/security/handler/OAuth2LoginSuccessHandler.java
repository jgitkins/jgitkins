package io.jgitkins.server.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.application.port.in.OAuthLoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final OAuthLoginUseCase oauthLoginUseCase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            log.warn("Unsupported authentication type: {}", authentication.getClass().getName());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication");
            return;
        }

        // get oidc(OPEN ID CONNECT) from token
        Object principal = oauthToken.getPrincipal();
        if (!(principal instanceof OidcUser oidcUser)) {
            log.warn("OIDC principal missing for provider {}", oauthToken.getAuthorizedClientRegistrationId());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OIDC principal required");
            return;
        }

        OAuthLoginCommand command = toCommand(oauthToken.getAuthorizedClientRegistrationId(), oidcUser);
        OAuthLoginResult result = oauthLoginUseCase.login(command);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private OAuthLoginCommand toCommand(String providerName, OidcUser oidcUser) {
        boolean verified = oidcUser.getEmailVerified() != null && oidcUser.getEmailVerified();
        String avatarUrl = oidcUser.getPicture() != null ? oidcUser.getPicture().toString() : null;
        return new OAuthLoginCommand(
                providerName,
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName(),
                verified,
                avatarUrl
        );
    }
}
