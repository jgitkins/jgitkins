package io.jgitkins.server.identity.access.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialIssueResult;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.identity.access.application.mapper.UserCredentialApplicationMapper;
import io.jgitkins.server.identity.access.application.port.out.ActiveAccountPolicyPort;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    @Mock
    private UserCredentialPersistencePort port;

    @Mock
    private ActiveAccountPolicyPort activeAccountPolicyPort;

    @Mock
    private PasswordEncoder encoder;

    private UserCredentialApplicationMapper userCredentialApplicationMapper = Mappers.getMapper(UserCredentialApplicationMapper.class);

    private UserCredentialService service;

    @BeforeEach
    void setUp() {
        service = new UserCredentialService(activeAccountPolicyPort, port, encoder, userCredentialApplicationMapper);
    }

    @Test
    void issueToken_issuesPlainCredentialAndPersistsHashedCredential() {
        when(activeAccountPolicyPort.requireActiveUserId()).thenReturn(1L);
        when(encoder.encode(any())).thenReturn("hashed");
        when(port.save(any(UserCredential.class))).thenAnswer(invocation -> {
            UserCredential credential = invocation.getArgument(0);
            return credential.withId(10L);
        });

        UserCredentialIssueResult result = service.issueCredential(new UserCredentialIssueCommand("token", "desc", null));

        assertNotNull(result.token());
        assertTrue(result.token().startsWith("jkpat_"));
        assertEquals(10L, result.credentialId());

        verify(encoder).encode(result.token());

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

        when(activeAccountPolicyPort.requireActiveUserId()).thenReturn(2L);
        when(port.findAllByUserIdAndProvider(2L, "PAT")).thenReturn(List.of(credential));
        List<UserCredentialSummary> result = service.getCredentials();

        assertEquals(1, result.size());
        UserCredentialSummary summary = result.get(0);
        assertEquals(7L, summary.id());
        assertEquals("PAT", summary.provider());
        assertEquals("n", summary.name());
        assertEquals("d", summary.description());
        assertEquals(createdAt, summary.createdAt());
        assertEquals(updatedAt, summary.updatedAt());
    }

    @Test
    void removeCredential_deletesByCredentialIdAndUserId() {
        when(activeAccountPolicyPort.requireActiveUserId()).thenReturn(3L);

        service.removeCredential(9L);

        verify(port).deleteByIdAndUserId(9L, 3L);
    }

    @Test
    void getCredentials_throwsUnauthorizedWhenCurrentUserMissing() {
        when(activeAccountPolicyPort.requireActiveUserId())
                .thenThrow(new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated"));

        assertThrows(ApplicationException.class, () -> service.getCredentials());
    }

    @Test
    void policyDenialsPropagateBeforeCredentialSideEffects() {
        when(activeAccountPolicyPort.requireActiveUserId())
                .thenThrow(new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "Access denied"));
        assertThrows(ApplicationException.class,
                () -> service.issueCredential(new UserCredentialIssueCommand("token", "desc", null)));
        assertThrows(ApplicationException.class, service::getCredentials);
        assertThrows(ApplicationException.class, () -> service.removeCredential(9L));
        verifyNoInteractions(port, encoder);
    }

    @Test
    void missingUserPropagatesBeforeCredentialSideEffects() {
        when(activeAccountPolicyPort.requireActiveUserId()).thenThrow(new UserNotFoundException());
        assertThrows(UserNotFoundException.class,
                () -> service.issueCredential(new UserCredentialIssueCommand("token", "desc", null)));
        verifyNoInteractions(port, encoder);
    }

    @Test
    void policyRunsBeforeEachCredentialSideEffect() {
        when(activeAccountPolicyPort.requireActiveUserId()).thenReturn(1L);
        when(encoder.encode(any())).thenReturn("hashed");
        when(port.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(port.findAllByUserIdAndProvider(1L, "PAT")).thenReturn(List.of());

        service.issueCredential(new UserCredentialIssueCommand("token", "desc", null));
        service.getCredentials();
        service.removeCredential(9L);

        InOrder order = org.mockito.Mockito.inOrder(activeAccountPolicyPort, encoder, port);
        order.verify(activeAccountPolicyPort).requireActiveUserId();
        order.verify(encoder).encode(any());
        order.verify(port).save(any(UserCredential.class));
        order.verify(activeAccountPolicyPort).requireActiveUserId();
        order.verify(port).findAllByUserIdAndProvider(1L, "PAT");
        order.verify(activeAccountPolicyPort).requireActiveUserId();
        order.verify(port).deleteByIdAndUserId(9L, 1L);
    }
}
