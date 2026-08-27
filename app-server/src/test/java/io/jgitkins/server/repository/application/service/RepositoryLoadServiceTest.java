package io.jgitkins.server.repository.application.service;

import static org.mockito.Mockito.verify;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryLoadServiceTest {

    @Mock
    private RepositoryQueryPort repositoryQueryPort;
    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;
    @Mock
    private UserNamespacePort userNamespacePort;

    private RepositoryLoadService service;

    @BeforeEach
    void setUp() {
        service = new RepositoryLoadService(repositoryQueryPort, userNamespacePort, repositoryAccessValidator);
    }

    @Test
    void loadRepository_validatesSameLoadedResult() {
        RepositoryResult result = new RepositoryResult(1L, null, null, null, null, null, null, null, null, null, null, false, null, null, null);
        when(repositoryQueryPort.loadRepository(1L)).thenReturn(Optional.of(result));

        RepositoryResult response = service.loadRepository(7L, 1L);

        assertEquals(1L, response.id());
        // The validator must see the very object being returned, not an equal one loaded again. A second
        // lookup could observe a different row than the caller receives, and would authorize state the
        // caller never sees.
        org.mockito.ArgumentCaptor<RepositoryResult> captor =
                org.mockito.ArgumentCaptor.forClass(RepositoryResult.class);
        verify(repositoryAccessValidator).validateReadAccess(captor.capture(), org.mockito.ArgumentMatchers.eq(7L));
        org.junit.jupiter.api.Assertions.assertSame(response, captor.getValue());
        verify(repositoryQueryPort, org.mockito.Mockito.times(1)).loadRepository(1L);
    }

    @Test
    void loadUserRepositories_passesRequester() {
        when(userNamespacePort.findUserIdByUsername("alice")).thenReturn(Optional.of(2L));
        when(repositoryQueryPort.loadUserRepositories("alice", 7L)).thenReturn(List.of());

        service.loadUserRepositories(7L, "alice");

        // The requester travels to the query rather than being resolved inside it: the visibility filter
        // is the authorization for a list, and it can only apply the caller it is given.
        verify(repositoryQueryPort).loadUserRepositories("alice", 7L);
    }

    @Test
    void loadUserRepositories_passesNullRequesterForAnonymous() {
        when(userNamespacePort.findUserIdByUsername("alice")).thenReturn(Optional.of(2L));
        when(repositoryQueryPort.loadUserRepositories("alice", null)).thenReturn(List.of());

        service.loadUserRepositories(null, "alice");

        // Anonymous is a value, not an error: the query narrows to the public subset. Substituting a
        // sentinel id would make the filter treat the caller as a user who might match a membership.
        verify(repositoryQueryPort).loadUserRepositories("alice", null);
    }

    @Test
    void loadRepositories_delegatesToQueryPortWithRequesterId() {
        List<RepositoryResult> expected = List.of(
                new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(2L, null, "mine", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(3L, null, "org", null, null, null, null, null, null, null, null, false, null, null, null)
        );

        when(repositoryQueryPort.loadVisibleRepositories(7L)).thenReturn(expected);

        List<RepositoryResult> response = service.loadRepositories(7L);

        assertEquals(expected, response);
    }

    @Test
    void loadUserRepositories_delegatesToQueryPortAfterUserExistenceCheck() {
        List<RepositoryResult> expected = List.of(
                new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null)
        );
        when(userNamespacePort.findUserIdByUsername("alice")).thenReturn(Optional.of(7L));
        when(repositoryQueryPort.loadUserRepositories("alice", 7L)).thenReturn(expected);

        List<RepositoryResult> response = service.loadUserRepositories(7L, "alice");

        assertEquals(expected, response);
    }
}
