package io.jgitkins.web.application.dto;

/**
 * What app-web sends app-server to exchange a completed OIDC login for an application token.
 *
 * <p>Carried the profile claims — subject, email, name, emailVerified, avatarUrl — until task 2.114.
 * app-server believed them, and the endpoint is reachable without credentials, so anyone could send
 * an email address and receive a token for that account. app-web having verified the claims before
 * sending them was true and unprovable.
 *
 * <p>Now app-web forwards the provider's id token and app-server verifies it. The claims travel
 * inside a signature instead of beside one.
 */
public record OAuthLoginRequest(
		String provider,
		String idToken
) {
}
