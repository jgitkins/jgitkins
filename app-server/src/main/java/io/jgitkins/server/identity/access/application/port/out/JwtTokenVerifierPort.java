package io.jgitkins.server.identity.access.application.port.out;

import io.jgitkins.server.identity.access.application.internal.JwtAuthenticationResult;
import java.util.Optional;

public interface JwtTokenVerifierPort {
    Optional<JwtAuthenticationResult> verify(String token);
}
