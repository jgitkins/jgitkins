package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

/**
 * Verifying an OAuth id token required reaching the identity provider, and we could not.
 *
 * <p>Answers 502, not 401. Verification fetches the provider's JWKS over the network on the first
 * decode and again when signing keys rotate, so an outage at the provider — or a DNS failure, or an
 * egress rule — surfaces at exactly the same point a bad signature does. Reporting it as 401 tells
 * every user their credentials are wrong during an incident that has nothing to do with their
 * credentials, and buries the incident itself in a metric nobody alerts on.
 */
public class OAuthProviderUnavailableException extends ApplicationException {

    public OAuthProviderUnavailableException(Throwable cause) {
        super(ApplicationProblemSpec.OAUTH_PROVIDER_UNAVAILABLE, "Identity provider is unavailable", cause);
    }
}
