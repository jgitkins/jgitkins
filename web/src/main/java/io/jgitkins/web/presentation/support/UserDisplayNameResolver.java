package io.jgitkins.web.presentation.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserDisplayNameResolver {

	public String resolve(Authentication authentication) {
		if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
			Object principal = oauthToken.getPrincipal();
			if (principal instanceof OidcUser oidcUser) {
				String name = oidcUser.getFullName();
				return StringUtils.hasText(name) ? name : oidcUser.getName();
			}
			if (principal instanceof OAuth2User oauth2User) {
				String name = oauth2User.getAttribute("name");
				return StringUtils.hasText(name) ? name : oauth2User.getName();
			}
		}
		return authentication != null ? authentication.getName() : "there";
	}
}
