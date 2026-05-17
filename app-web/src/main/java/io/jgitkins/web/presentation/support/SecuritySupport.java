package io.jgitkins.web.presentation.support;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecuritySupport {

	private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

	public boolean isAuthenticated(Authentication authentication) {
		return authentication != null
				&& authentication.isAuthenticated()
				&& !TRUST_RESOLVER.isAnonymous(authentication);
	}
}
