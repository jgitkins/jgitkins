package io.jgitkins.server.identity.access.adapter.in.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.adapter.out.security.PatTokenAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class PatAuthenticationProviderTest {
    @Test
    void supportsUsernamePasswordAuthentication() {
        PatAuthenticationProvider provider = new PatAuthenticationProvider(mock(PatTokenAuthenticationService.class));

        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void delegatesAuthenticationToPatTokenService() {
        PatTokenAuthenticationService service = mock(PatTokenAuthenticationService.class);
        PatAuthenticationProvider provider = new PatAuthenticationProvider(service);
        Authentication expected = mock(Authentication.class);
        UsernamePasswordAuthenticationToken request =
                new UsernamePasswordAuthenticationToken("alice", "token");
        when(service.authenticate("alice", "token")).thenReturn(expected);

        assertSame(expected, provider.authenticate(request));
    }
}
