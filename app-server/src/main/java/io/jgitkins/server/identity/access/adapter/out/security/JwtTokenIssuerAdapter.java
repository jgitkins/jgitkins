package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuerAdapter implements TokenIssuerPort {
    private final JwtTokenCodec codec;

    @Override
    public String issueToken(Long userId, List<String> roles) {
        return codec.issueToken(userId, roles);
    }
}
