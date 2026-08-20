package io.jgitkins.server.repository.infrastructure.adapter.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryActorAclAdapterTest {

    @Mock private CurrentUserPort delegate;

    @Test
    void delegatesAuthenticatedIdentity() {
        when(delegate.resolveCurrentUserId()).thenReturn(Optional.of(11L));

        assertThat(new RepositoryActorAclAdapter(delegate).resolveCurrentUserId())
                .contains(11L);
        verify(delegate).resolveCurrentUserId();
    }

    @Test
    void preservesUnauthenticatedIdentity() {
        when(delegate.resolveCurrentUserId()).thenReturn(Optional.empty());

        assertThat(new RepositoryActorAclAdapter(delegate).resolveCurrentUserId())
                .isEmpty();
        verify(delegate).resolveCurrentUserId();
    }
}
