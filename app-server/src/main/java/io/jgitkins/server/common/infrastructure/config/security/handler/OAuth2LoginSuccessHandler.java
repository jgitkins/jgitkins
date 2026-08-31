package io.jgitkins.server.common.infrastructure.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;
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
/**
 * Completes an authorization-code login performed against app-server itself.
 *
 * <p>Hands the use case the raw id token rather than the claims it just read off {@link OidcUser}.
 * The claims here are trustworthy — this handler only runs after Spring Security completed the
 * handshake — so the token is verified a second time on the way through. That is the point: the use
 * case has exactly one way to learn who is logging in, and an exception for the caller that happens
 * to be inside the same process is how the rule stops being a rule. One extra verification per
 * login, against a decoder that caches the provider's keys.
 *
 * <p>In the deployed topology this path is not exercised — the browser reaches
 * {@code /oauth2/authorization/google} on app-web, which then calls app-server's REST endpoint. It
 * is kept working rather than deleted because removing a login path is not what task 2.114 was for.
 */
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

        Object principal = oauthToken.getPrincipal();
        if (!(principal instanceof OidcUser oidcUser)) {
            log.warn("OIDC principal missing for provider {}", oauthToken.getAuthorizedClientRegistrationId());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OIDC principal required");
            return;
        }

        OAuthLoginCommand command = new OAuthLoginCommand(
                oauthToken.getAuthorizedClientRegistrationId(),
                oidcUser.getIdToken().getTokenValue());
        OAuthLoginResult result = oauthLoginUseCase.login(command);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
