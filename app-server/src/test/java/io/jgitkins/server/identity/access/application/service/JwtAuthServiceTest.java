package io.jgitkins.server.identity.access.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.identity.access.application.contract.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.port.out.JwtTokenVerifierPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtAuthServiceTest {
    @Test
    void authenticateDelegatesPresentResult() {
        JwtTokenVerifierPort verifier = mock(JwtTokenVerifierPort.class);
        when(verifier.verify("token")).thenReturn(Optional.of(new JwtAuthenticationResult(7L, List.of("ROLE_USER"))));
        assertEquals(7L, new JwtAuthService(verifier).authenticate("token").orElseThrow().userId());
    }

    @Test
    void authenticateFailsClosedForBlankAndNullVerifierResult() {
        JwtTokenVerifierPort verifier = mock(JwtTokenVerifierPort.class);
        when(verifier.verify("token")).thenReturn(null);
        JwtAuthService service = new JwtAuthService(verifier);
        assertTrue(service.authenticate(null).isEmpty());
        assertTrue(service.authenticate(" ").isEmpty());
        assertTrue(service.authenticate("token").isEmpty());
    }
}
