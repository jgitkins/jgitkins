package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.application.dto.result.UserCredentialIssueResult;
import io.jgitkins.server.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.mapper.UserCredentialApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.domain.model.UserCredential;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    @Mock
    private UserCredentialPersistencePort port;

    @Mock
    private CurrentUserPort currentUserPersistencePort;

    @Mock
    private PasswordEncoder encoder;

    private UserCredentialApplicationMapper userCredentialApplicationMapper = Mappers.getMapper(UserCredentialApplicationMapper.class);

    private UserCredentialService service;

    @BeforeEach
    void setUp() {
        service = new UserCredentialService(currentUserPersistencePort, port, encoder, userCredentialApplicationMapper);
    }

    @Test
    void issueToken_issuesPlainCredentialAndPersistsHashedCredential() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        when(encoder.encode(any())).thenReturn("hashed");
        when(port.save(any(UserCredential.class))).thenAnswer(invocation -> {
            UserCredential credential = invocation.getArgument(0);
            return credential.withId(10L);
        });

        UserCredentialIssueResult result = service.issueCredential(new UserCredentialIssueCommand("token", "desc", null));

        assertNotNull(result.getToken());
        assertTrue(result.getToken().startsWith("jkpat_"));
        assertEquals(10L, result.getCredentialId());

        verify(encoder).encode(result.getToken());

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(port).save(captor.capture());
        UserCredential saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("PAT", saved.getProvider());
        assertEquals("token", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals("hashed", saved.getPasswordHash());
    }

    @Test
    void getCredentials_mapsToSummary() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 0, 0);
        UserCredential credential = UserCredential.rehydrate(7L, 2L, "PAT", "n", "d", "hash", createdAt, updatedAt);

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(2L));
        when(port.findAllByUserIdAndProvider(2L, "PAT")).thenReturn(List.of(credential));
        List<UserCredentialSummary> result = service.getCredentials();

        assertEquals(1, result.size());
        UserCredentialSummary summary = result.get(0);
        assertEquals(7L, summary.getId());
        assertEquals("PAT", summary.getProvider());
        assertEquals("n", summary.getName());
        assertEquals("d", summary.getDescription());
        assertEquals(createdAt, summary.getCreatedAt());
        assertEquals(updatedAt, summary.getUpdatedAt());
    }

    @Test
    void removeCredential_deletesByCredentialIdAndUserId() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(3L));

        service.removeCredential(9L);

        verify(port).deleteByIdAndUserId(9L, 3L);
    }

    @Test
    void getCredentials_throwsUnauthorizedWhenCurrentUserMissing() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.empty());

        JgitkinsException exception = assertThrows(JgitkinsException.class, () -> service.getCredentials());

        assertSame(ApplicationErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }
}
