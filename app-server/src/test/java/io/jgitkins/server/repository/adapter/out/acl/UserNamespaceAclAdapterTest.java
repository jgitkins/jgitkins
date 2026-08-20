package io.jgitkins.server.repository.adapter.out.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNamespaceAclAdapterTest {

    @Mock private UserQueryPort delegate;

    @Test
    void mapsScalarNamespaceLookupWithoutUserAggregate() {
        when(delegate.findUserIdByUsername("alice")).thenReturn(Optional.of(7L));

        assertThat(new UserNamespaceAclAdapter(delegate).findUserIdByUsername("alice"))
                .contains(7L);
        verify(delegate).findUserIdByUsername("alice");
    }

    @Test
    void preservesMissingNamespace() {
        when(delegate.findUserIdByUsername("missing")).thenReturn(Optional.empty());

        assertThat(new UserNamespaceAclAdapter(delegate).findUserIdByUsername("missing"))
                .isEmpty();
        verify(delegate).findUserIdByUsername("missing");
    }
}
