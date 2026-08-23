package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.identity.access.application.dto.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.port.out.JwtTokenVerifierPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenVerifierAdapter implements JwtTokenVerifierPort {
    private final JwtTokenCodec codec;

    @Override
    public Optional<JwtAuthenticationResult> verify(String token) {
        Optional<JwtAuthenticationResult> result = codec.verify(token);
        return result == null ? Optional.empty() : result;
    }
}
