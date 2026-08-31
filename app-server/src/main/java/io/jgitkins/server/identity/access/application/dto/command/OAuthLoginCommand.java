package io.jgitkins.server.identity.access.application.dto.command;

/**
 * What the application needs to log someone in: a provider, and that provider's id token.
 *
 * <p>Not the identity itself. The identity is derived inside the use case, from a token the verifier
 * port has checked — see {@code OAuthLoginRequest} for what this command used to carry and why it no
 * longer does.
 */
public record OAuthLoginCommand(
        String provider,
        String idToken
) {
}
