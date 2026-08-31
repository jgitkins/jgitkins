package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

/**
 * The identity claimed by an OAuth login request could not be established from a verified token.
 *
 * <p>One exception for every reason the verification can fail — unknown provider, malformed token,
 * bad signature, wrong audience, expired, missing subject. The caller learns that the login did not
 * work and nothing else. Splitting these would hand an unauthenticated caller a probe: which
 * providers are configured, whether a token was well-formed but rejected, whether it was ours but
 * stale. None of that helps a legitimate client, which can only do one thing about any of them.
 *
 * <p>Deliberately not thrown for a failure to reach the provider — see
 * {@link OAuthProviderUnavailableException}. That distinction is the one worth making, because the
 * two have different audiences: this one is for the user, that one is for whoever is on call.
 */
public class OAuthIdentityNotVerifiedException extends ApplicationException {

    /**
     * Why verification failed, for the log only. Never reaches the response body: the message the
     * caller sees is the constant above, so this can name the actual cause without turning the
     * endpoint into an oracle.
     */
    private final String reason;

    public OAuthIdentityNotVerifiedException(String reason) {
        super(ApplicationProblemSpec.OAUTH_IDENTITY_UNVERIFIED, "OAuth identity could not be verified");
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
