package io.jgitkins.server.repository.application.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryLookupServiceTest {
    @Mock private RepositoryRepository repositoryRepository;
    @Mock private UserQueryPort userQueryPort;
    @Mock private OrganizeQueryPort organizePort;

    @Test
    void resolveByOwner_userNamespace_usesScalarUserIdLookup() {
        when(userQueryPort.findUserIdByUsername("alice")).thenReturn(Optional.empty());
        RepositoryLookupService service = new RepositoryLookupService(repositoryRepository, userQueryPort, organizePort);
        assertTrue(service.resolveByOwner(io.jgitkins.server.shared.domain.model.vo.OwnerType.USER, "alice", "repo").isEmpty());
        verify(userQueryPort).findUserIdByUsername("alice");
    }
}
