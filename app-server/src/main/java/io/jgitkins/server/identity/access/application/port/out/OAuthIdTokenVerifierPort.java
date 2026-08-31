package io.jgitkins.server.identity.access.application.port.out;

import io.jgitkins.server.identity.access.application.dto.result.VerifiedOAuthIdentity;

/**
 * Turns a provider name and a raw id token into an identity the application is allowed to believe.
 *
 * <p>The application layer holds no other way to learn who is logging in. That is the whole point:
 * before task 2.114 the login command carried provider, subject and email as plain fields supplied
 * by the caller, and nothing downstream could tell a claim apart from a fact.
 */
public interface OAuthIdTokenVerifierPort {

    /**
     * @throws io.jgitkins.server.identity.access.application.exception.OAuthIdentityNotVerifiedException
     *     the token is absent, malformed, not signed by the provider, not issued for us, expired, or
     *     carries no subject — and for an unknown provider, so the answer cannot be used to
     *     enumerate which are configured
     * @throws io.jgitkins.server.identity.access.application.exception.OAuthProviderUnavailableException
     *     the provider could not be reached to verify the signature
     */
    VerifiedOAuthIdentity verify(String provider, String idToken);
}
