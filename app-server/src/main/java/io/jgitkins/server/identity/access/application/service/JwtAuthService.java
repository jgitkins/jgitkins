package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.internal.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.port.out.JwtTokenVerifierPort;
import java.util.Optional;

public class JwtAuthService {
    private final JwtTokenVerifierPort verifier;

    public JwtAuthService(JwtTokenVerifierPort verifier) {
        this.verifier = verifier;
    }

    public Optional<JwtAuthenticationResult> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<JwtAuthenticationResult> result = verifier.verify(token);
        return result == null ? Optional.empty() : result;
    }
}
