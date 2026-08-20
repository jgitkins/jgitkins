package io.jgitkins.server.collaboration.adapter.out.acl;

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
class InProcessUserIdentityAdapterTest {

    @Mock private CurrentUserPort currentUserPort;

    @Test
    void returnsAuthenticatedIdWithoutStatusLookup() {
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(42L));

        assertThat(new InProcessUserIdentityAdapter(currentUserPort).resolveCurrentActiveUserId())
                .contains(42L);
        verify(currentUserPort).resolveCurrentUserId();
    }

    @Test
    void preservesUnauthenticatedEmptyResult() {
        when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.empty());

        assertThat(new InProcessUserIdentityAdapter(currentUserPort).resolveCurrentActiveUserId())
                .isEmpty();
        verify(currentUserPort).resolveCurrentUserId();
    }
}
