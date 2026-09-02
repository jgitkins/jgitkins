package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.identity.access.application.contract.result.VerifiedOAuthIdentity;
import io.jgitkins.server.identity.access.application.exception.OAuthIdentityNotVerifiedException;
import io.jgitkins.server.identity.access.application.exception.OAuthProviderUnavailableException;
import io.jgitkins.server.identity.access.application.port.out.OAuthIdTokenVerifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.nimbusds.jose.RemoteKeySourceException;
import java.io.IOException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Verifies an OIDC id token against the provider that issued it.
 *
 * <p>Uses the {@link ClientRegistration} the application already configures for the login flow, so
 * the issuer, the JWKS location and the audience all come from one place rather than a second copy
 * that can drift. {@code OidcIdTokenDecoderFactory} builds the decoder: it fetches the signing keys
 * from the registration's {@code jwkSetUri} and installs {@code OidcIdTokenValidator}, which checks
 * {@code iss}, {@code aud}, {@code azp}, {@code exp} and {@code iat}. The audience is the client id,
 * and app-server and app-web read it from the same environment variable, so a token minted for the
 * web client validates here without any additional configuration.
 *
 * <p><strong>Two failures, two answers.</strong> A token we can read and reject is the caller's
 * problem (401). A provider we cannot reach is ours (502) — key fetching happens over the network on
 * the first decode and again on rotation, so both failures arrive at the same call and only the
 * exception type tells them apart. See {@link OAuthProviderUnavailableException}.
 *
 * <p>The reason a verification failed is logged and never returned. An unauthenticated caller who
 * could tell "unknown provider" from "bad signature" from "wrong audience" would have a probe into
 * the deployment's configuration; a caller who cannot has lost nothing, since the remedy is the same
 * for all of them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OidcIdTokenVerifierAdapter implements OAuthIdTokenVerifierPort {

    private static final int MAX_CAUSE_DEPTH = 32;

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory;

    @Override
    public VerifiedOAuthIdentity verify(String provider, String idToken) {
        if (provider == null || provider.isBlank()) {
            throw unverified("provider is blank");
        }
        if (idToken == null || idToken.isBlank()) {
            throw unverified("id token is blank");
        }

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider);
        if (registration == null) {
            // The provider name is caller-supplied and reaches a log line, so it is not echoed:
            // the route is permitAll, there is no logback pattern overriding the default %msg, and
            // a value containing a newline would write forged entries into the log.
            throw unverified("no client registration for the requested provider");
        }

        Jwt jwt = decode(registration, idToken);

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            // OIDC requires sub, so this is a provider that is not one, or a token that is not an
            // id token. Either way there is no identity here to act on.
            throw unverified("verified token carries no subject, provider="
                    + registration.getRegistrationId());
        }

        return new VerifiedOAuthIdentity(
                registration.getRegistrationId(),
                subject,
                jwt.getClaimAsString(StandardClaimNames.EMAIL),
                Boolean.TRUE.equals(jwt.getClaim(StandardClaimNames.EMAIL_VERIFIED)),
                jwt.getClaimAsString(StandardClaimNames.NAME),
                jwt.getClaimAsString(StandardClaimNames.PICTURE));
    }

    private Jwt decode(ClientRegistration registration, String idToken) {
        JwtDecoder decoder;
        try {
            decoder = idTokenDecoderFactory.createDecoder(registration);
        } catch (OAuth2AuthenticationException e) {
            // OidcIdTokenDecoderFactory refuses to build a decoder when the registration has no
            // jwkSetUri, no client secret for a MAC algorithm, or an algorithm it does not support.
            // That is our configuration being wrong, not the caller's token, so it answers the same
            // way an unreachable provider does.
            throw cannotVerify(registration, e);
        }
        try {
            return decoder.decode(idToken);
        } catch (JwtException e) {
            // Both outcomes arrive as JwtException, and only the cause tells them apart.
            // NimbusJwtDecoder wraps a RemoteKeySourceException -- the JWKS could not be fetched, or
            // came back malformed -- in a plain JwtException, exactly as it does for a bad
            // signature. Reading the type alone would report every provider outage as invalid
            // credentials, which is the one thing this class exists to avoid.
            if (isUpstreamFailure(e)) {
                throw cannotVerify(registration, e);
            }
            throw unverified("decode rejected the token");
        }
    }

    /**
     * Walks the cause chain, bounded.
     *
     * <p>The bound is not decoration. The self-reference check alone ({@code getCause() == cause})
     * misses a two-element cycle, and a cause chain that loops would hang this thread inside a login
     * request rather than answering it. A depth limit terminates on any cycle length, and no real
     * exception chain from a JWKS fetch is anywhere near this deep.
     */
    private static boolean isUpstreamFailure(Throwable failure) {
        int depth = 0;
        for (Throwable cause = failure; cause != null && depth < MAX_CAUSE_DEPTH; cause = cause.getCause()) {
            if (cause instanceof RemoteKeySourceException || cause instanceof IOException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
            depth++;
        }
        return false;
    }

    private OAuthIdentityNotVerifiedException unverified(String reason) {
        log.warn("oauth login denied: {}", reason);
        return new OAuthIdentityNotVerifiedException(reason);
    }

    private OAuthProviderUnavailableException cannotVerify(ClientRegistration registration, Exception cause) {
        log.error("oauth login could not verify: provider=[{}] jwkSetUri=[{}] could not be used",
                registration.getRegistrationId(),
                registration.getProviderDetails().getJwkSetUri(),
                cause);
        return new OAuthProviderUnavailableException(cause);
    }
}
