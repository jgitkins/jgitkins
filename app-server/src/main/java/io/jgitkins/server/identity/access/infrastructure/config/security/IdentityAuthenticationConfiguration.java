package io.jgitkins.server.identity.access.infrastructure.config.security;

import io.jgitkins.server.identity.access.application.port.out.JwtTokenVerifierPort;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityAuthenticationConfiguration {
    @Bean
    JwtAuthService jwtAuthService(JwtTokenVerifierPort verifier) {
        return new JwtAuthService(verifier);
    }
}
