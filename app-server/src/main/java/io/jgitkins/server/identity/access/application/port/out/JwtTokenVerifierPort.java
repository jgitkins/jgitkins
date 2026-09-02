package io.jgitkins.server.identity.access.application.port.out;

import io.jgitkins.server.identity.access.application.contract.external.JwtAuthenticationResult;
import java.util.Optional;

public interface JwtTokenVerifierPort {
    Optional<JwtAuthenticationResult> verify(String token);
}
