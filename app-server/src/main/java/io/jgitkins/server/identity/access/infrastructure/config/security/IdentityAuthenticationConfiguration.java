package io.jgitkins.server.identity.access.infrastructure.config.security;

import io.jgitkins.server.identity.access.application.port.out.JwtTokenVerifierPort;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

@Configuration
public class IdentityAuthenticationConfiguration {
    @Bean
    JwtAuthService jwtAuthService(JwtTokenVerifierPort verifier) {
        return new JwtAuthService(verifier);
    }

    /**
     * The decoder the OAuth login endpoint verifies id tokens with.
     *
     * <p>Spring Security builds one of these internally for the authorization-code flow but does not
     * publish it, so verifying a token outside that flow needs it declared. Declaring it here rather
     * than constructing it inside the adapter keeps the adapter injectable with a decoder that does
     * not reach the network, which is what its tests use.
     *
     * <p>{@code OidcIdTokenDecoderFactory} caches one decoder per registration and installs
     * {@code OidcIdTokenValidator} (iss, aud, azp, exp, iat) plus signature verification against the
     * registration's {@code jwkSetUri}.
     */
    @Bean
    JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        return new OidcIdTokenDecoderFactory();
    }
}
