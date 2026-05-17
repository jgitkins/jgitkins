package io.jgitkins.web.presentation.support;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RepositoryUserProfileResolver {

	public RepositoryUserProfile resolve(Authentication authentication) {
		if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
			Object principal = oauthToken.getPrincipal();
			if (principal instanceof OidcUser oidcUser) {
				String name = StringUtils.hasText(oidcUser.getFullName()) ? oidcUser.getFullName() : oidcUser.getName();
				return new RepositoryUserProfile(name, oidcUser.getEmail());
			}
			if (principal instanceof OAuth2User oauth2User) {
				String name = oauth2User.getAttribute("name");
				String email = oauth2User.getAttribute("email");
				String fallbackName = StringUtils.hasText(name) ? name : oauth2User.getName();
				return new RepositoryUserProfile(fallbackName, email);
			}
		}
		return new RepositoryUserProfile("Personal", null);
	}

	public boolean isAuthenticated(Authentication authentication) {
		return authentication != null
				&& !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.isAuthenticated();
	}
}
