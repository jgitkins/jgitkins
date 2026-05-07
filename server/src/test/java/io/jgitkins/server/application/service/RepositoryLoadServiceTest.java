package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
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
    private CurrentUserPort currentUserPort;
    @Mock
    private UserPersistencePort userPort;

    private RepositoryLoadService service;

    @BeforeEach
    void setUp() {
        service = new RepositoryLoadService(repositoryQueryPort, currentUserPort, userPort);
    }

    @Test
    void loadRepository_returnsQueryResult() {
        RepositoryResult result = new RepositoryResult(1L, null, null, null, null, null, null, null, null, null, null, false, null, null, null);
        when(repositoryQueryPort.loadRepository(1L)).thenReturn(Optional.of(result));

        RepositoryResult response = service.loadRepository(1L);

        assertEquals(1L, response.id());
    }

    @Test
    void loadRepositories_delegatesToQueryPortWithRequesterId() {
        List<RepositoryResult> expected = List.of(
                new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(2L, null, "mine", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(3L, null, "org", null, null, null, null, null, null, null, null, false, null, null, null)
        );

        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(repositoryQueryPort.loadVisibleRepositories(7L)).thenReturn(expected);

        List<RepositoryResult> response = service.loadRepositories();

        assertEquals(expected, response);
    }

    @Test
    void loadUserRepositories_delegatesToQueryPortAfterUserExistenceCheck() {
        List<RepositoryResult> expected = List.of(
                new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null)
        );
        when(userPort.findUserIdByUsername("alice")).thenReturn(Optional.of(7L));
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(9L));
        when(repositoryQueryPort.loadUserRepositories("alice", 9L)).thenReturn(expected);

        List<RepositoryResult> response = service.loadUserRepositories("alice");

        assertEquals(expected, response);
    }
}
