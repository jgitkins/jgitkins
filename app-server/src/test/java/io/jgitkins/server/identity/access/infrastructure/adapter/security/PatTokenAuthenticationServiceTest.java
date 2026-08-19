package io.jgitkins.server.identity.access.infrastructure.adapter.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PatTokenAuthenticationServiceTest {
    @Mock private UserQueryPort userQueryPort;
    @Mock private UserCredentialPersistencePort credentialPort;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void authenticate_usesScalarUserIdLookup() {
        when(userQueryPort.findUserIdByUsername("alice")).thenReturn(Optional.empty());
        PatTokenAuthenticationService service = new PatTokenAuthenticationService(userQueryPort, credentialPort, passwordEncoder);
        assertThrows(UsernameNotFoundException.class, () -> service.authenticate("alice", "jkpat_token"));
        verify(userQueryPort).findUserIdByUsername("alice");
    }
}
