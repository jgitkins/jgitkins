package io.jgitkins.web.infrastructure.config.security.handler;

import io.jgitkins.web.application.port.out.AppSessionTokenPort;
import io.jgitkins.web.application.dto.OAuthLoginRequest;
import io.jgitkins.web.application.dto.ServerOAuthLoginResult;
import io.jgitkins.web.application.dto.ServerOAuthLoginResult.ServerUserProfile;
import io.jgitkins.web.application.port.out.AppTokenIssuePort;
import io.jgitkins.web.application.common.UserStatus;
import io.jgitkins.web.presentation.support.SessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final AppTokenIssuePort appTokenIssuePort;
	private final AppSessionTokenPort appSessionTokenPort;
	private final SessionSupport sessionSupport;

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

		OAuthLoginRequest tokenRequest = new OAuthLoginRequest(
				oauthToken.getAuthorizedClientRegistrationId(),
				oidcUser.getSubject(),
				oidcUser.getEmail(),
				oidcUser.getFullName(),
				oidcUser.getEmailVerified() != null && oidcUser.getEmailVerified(),
				oidcUser.getPicture() != null ? oidcUser.getPicture().toString() : null
		);
        // try login (issue jwt token)
		ServerOAuthLoginResult result = appTokenIssuePort.issueOAuthLoginToken(tokenRequest);
		appSessionTokenPort.store(request, result.appToken());
		setUserState(request, result);
		response.sendRedirect("/");
	}

	private void setUserState(HttpServletRequest request, ServerOAuthLoginResult result) {
		if (result.user() == null) {
			return;
		}
		ServerUserProfile user = result.user();
		sessionSupport.storeUserState(request, user.username(), UserStatus.from(user.status()));
	}
}
