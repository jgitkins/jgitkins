package io.jgitkins.server.shared.application.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryNamespaceResolverTest {
    @Mock private OrganizeQueryPort organizePort;
    @Mock private UserQueryPort userQueryPort;

    @Test
    void resolve_userOwner_usesScalarUsernameLookup() {
        when(userQueryPort.findUsernameById(7L)).thenReturn(Optional.of("alice"));
        RepositoryNamespaceResolver resolver = new RepositoryNamespaceResolver(organizePort, userQueryPort);
        assertEquals("alice", resolver.resolve(OwnerType.USER, RepositoryOwnerId.of(7L)));
        verify(userQueryPort).findUsernameById(7L);
    }
}
