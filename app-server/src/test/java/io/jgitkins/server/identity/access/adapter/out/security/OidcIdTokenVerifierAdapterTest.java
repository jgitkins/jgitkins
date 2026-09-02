package io.jgitkins.server.identity.access.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jgitkins.server.identity.access.application.contract.external.VerifiedOAuthIdentity;
import io.jgitkins.server.identity.access.application.exception.OAuthIdentityNotVerifiedException;
import io.jgitkins.server.identity.access.application.exception.OAuthProviderUnavailableException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * The verifier's contract, exercised against tokens this test signs itself.
 *
 * <p>No network: the decoder is built from an in-memory public key rather than from the
 * registration's {@code jwkSetUri}, but it carries the same validators production installs.
 *
 * <p>Every rejection case here was a success case before task 2.114, because there was no token to
 * reject — the caller sent the identity as plain fields and the server used them. The one that
 * matters most is {@link #tokenSignedByAnotherKeyIsRejected()}: it is the difference between a
 * signature we checked and a claim we were handed.
 */
class OidcIdTokenVerifierAdapterTest {

    private static final String ISSUER = "https://issuer.example.test";
    private static final String CLIENT_ID = "jgitkins-test-client";

    private static RSAKey signingKey;
    private static RSAKey otherKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("k1").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("k2").generate();
    }

    @Test
    void verifiedTokenYieldsTheIdentityItsClaimsCarry() {
        VerifiedOAuthIdentity identity = adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims
                        .subject("sub-42")
                        .claim("email", "person@example.test")
                        .claim("email_verified", true)
                        .claim("name", "A Person")
                        .claim("picture", "https://img.example.test/a.png")));

        assertThat(identity).isEqualTo(new VerifiedOAuthIdentity(
                "google", "sub-42", "person@example.test", true, "A Person",
                "https://img.example.test/a.png"));
    }

    @Test
    void tokenSignedByAnotherKeyIsRejected() {
        // The whole point. A token the provider did not sign carries no identity, however
        // well-formed its claims are.
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("google", idToken(otherKey, claims -> claims.subject("sub-42"))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims
                        .subject("sub-42")
                        .issueTime(Date.from(Instant.now().minusSeconds(7200)))
                        .expirationTime(Date.from(Instant.now().minusSeconds(3600))))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void tokenIssuedForAnotherAudienceIsRejected() {
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims
                        .subject("sub-42")
                        .audience(List.of("some-other-client")))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims
                        .subject("sub-42")
                        .issuer("https://attacker.example.test"))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void verifiedTokenWithoutSubjectIsRejected() {
        // OIDC requires sub. A token without one is not an id token, so there is no identity to act
        // on even though the signature checks out.
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims.claim("email", "x@example.test"))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void unknownProviderIsRejectedAsUnverified() {
        // Not a 404 and not a distinct code: an unauthenticated caller must not be able to read the
        // set of configured providers off the response.
        assertThatThrownBy(() -> adapter(decoderFor(signingKey))
                .verify("not-configured", idToken(signingKey, claims -> claims.subject("sub-42"))))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void blankTokenIsRejected() {
        assertThatThrownBy(() -> adapter(decoderFor(signingKey)).verify("google", "  "))
                .isInstanceOf(OAuthIdentityNotVerifiedException.class);
    }

    @Test
    void missingEmailVerifiedClaimCountsAsUnverified() {
        // Absent is not true. Providers differ on whether they send it, and the account-linking rule
        // reads this field to decide whether an address may attach to an existing account.
        VerifiedOAuthIdentity identity = adapter(decoderFor(signingKey))
                .verify("google", idToken(signingKey, claims -> claims
                        .subject("sub-42")
                        .claim("email", "person@example.test")));

        assertThat(identity.emailVerified()).isFalse();
    }

    @Test
    void anUnreachableJwkSetIsNotReportedAsBadCredentials() {
        // The decoder is a real NimbusJwtDecoder pointed at a closed port, so the exception under
        // test is the one the library actually produces rather than one this test invented.
        //
        // That distinction is the whole test. The first version of this class stubbed the factory to
        // throw JwtDecoderInitializationException, and it passed -- but NimbusJwtDecoder 6.2.2 never
        // throws that type: a RemoteKeySourceException comes back wrapped in a plain JwtException,
        // the same type a bad signature produces. The green test was asserting a branch production
        // could not reach, while production answered 401 to every provider outage.
        JwtDecoderFactory<ClientRegistration> unreachable = registration ->
                NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:1/jwks").build();

        assertThatThrownBy(() -> adapter(unreachable)
                .verify("google", idToken(signingKey, claims -> claims.subject("sub-42"))))
                .isInstanceOf(OAuthProviderUnavailableException.class);
    }

    @Test
    void aRegistrationWithNoJwkSetUriIsNotReportedAsBadCredentials() {
        // OidcIdTokenDecoderFactory refuses to build a decoder and throws OAuth2AuthenticationException,
        // which is neither a JwtException nor anything the token caused. Our configuration is wrong;
        // the caller's credentials are not.
        ClientRegistration incomplete = ClientRegistration.withRegistrationId("google")
                .clientId(CLIENT_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("openid")
                .authorizationUri(ISSUER + "/authorize")
                .tokenUri(ISSUER + "/token")
                .issuerUri(ISSUER)
                .userInfoUri(ISSUER + "/userinfo")
                .userNameAttributeName("sub")
                .build();
        ClientRegistrationRepository repository = id -> "google".equals(id) ? incomplete : null;
        OidcIdTokenVerifierAdapter adapter =
                new OidcIdTokenVerifierAdapter(repository, new OidcIdTokenDecoderFactory());

        assertThatThrownBy(() -> adapter.verify("google",
                idToken(signingKey, claims -> claims.subject("sub-42"))))
                .isInstanceOf(OAuthProviderUnavailableException.class);
    }

    private OidcIdTokenVerifierAdapter adapter(JwtDecoderFactory<ClientRegistration> decoderFactory) {
        ClientRegistration google = registration();
        ClientRegistrationRepository repository = registrationId ->
                "google".equals(registrationId) ? google : null;
        return new OidcIdTokenVerifierAdapter(repository, decoderFactory);
    }

    /**
     * A decoder that checks the signature against an in-memory key and then applies the same
     * validations {@code OidcIdTokenDecoderFactory} installs in production — issuer, audience, azp,
     * subject, expiry — so the rejection cases below exercise the real rules rather than a
     * simplified stand-in. Only the key source differs, which is what keeps this test off the
     * network.
     */
    private JwtDecoderFactory<ClientRegistration> decoderFor(RSAKey key) {
        return registration -> {
            NimbusJwtDecoder decoder;
            try {
                decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
                        .signatureAlgorithm(SignatureAlgorithm.RS256)
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    new OidcIdTokenValidator(registration)));
            return decoder;
        };
    }

    private ClientRegistration registration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(CLIENT_ID)
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("openid")
                .authorizationUri(ISSUER + "/authorize")
                .tokenUri(ISSUER + "/token")
                .jwkSetUri(ISSUER + "/jwks")
                .issuerUri(ISSUER)
                .userInfoUri(ISSUER + "/userinfo")
                .userNameAttributeName("sub")
                .build();
    }

    private String idToken(RSAKey key, java.util.function.UnaryOperator<JWTClaimsSet.Builder> customise) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .audience(List.of(CLIENT_ID))
                    .issueTime(Date.from(Instant.now().minusSeconds(30)))
                    .expirationTime(Date.from(Instant.now().plusSeconds(600)));
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                    customise.apply(claims).build());
            jwt.sign(new RSASSASigner(key.toRSAPrivateKey()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
