package io.jgitkins.server.identity.access.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerAdapterTest {
    @Test
    void delegatesIssueTokenToCodec() {
        JwtTokenCodec codec = mock(JwtTokenCodec.class);
        when(codec.issueToken(3L, List.of("ROLE_USER"))).thenReturn("jwt");
        TokenIssuerPort issuer = new JwtTokenIssuerAdapter(codec);
        assertEquals("jwt", issuer.issueToken(3L, List.of("ROLE_USER")));
        verify(codec).issueToken(3L, List.of("ROLE_USER"));
    }
}
