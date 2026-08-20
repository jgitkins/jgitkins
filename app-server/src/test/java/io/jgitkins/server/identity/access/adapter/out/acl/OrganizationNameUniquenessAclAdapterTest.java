package io.jgitkins.server.identity.access.adapter.out.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrganizationNameUniquenessAclAdapterTest {
    @Test
    void invalidNameIsAvailableWithoutForeignQuery() {
        OrganizeQueryPort queryPort = mock(OrganizeQueryPort.class);
        assertThat(new OrganizationNameUniquenessAclAdapter(queryPort).isAvailableForUsername("bad name")).isTrue();
        verifyNoInteractions(queryPort);
    }

    @Test
    void existingAndMissingNamesMapToUnavailableAndAvailable() {
        OrganizeQueryPort queryPort = mock(OrganizeQueryPort.class);
        OrganizeName name = OrganizeName.from("alice");
        when(queryPort.findByName(name)).thenReturn(Optional.of(mock(Organize.class)));
        assertThat(new OrganizationNameUniquenessAclAdapter(queryPort).isAvailableForUsername("alice")).isFalse();
        when(queryPort.findByName(name)).thenReturn(Optional.empty());
        assertThat(new OrganizationNameUniquenessAclAdapter(queryPort).isAvailableForUsername("alice")).isTrue();
    }

    @Test
    void preservesDomainException() {
        OrganizeQueryPort queryPort = mock(OrganizeQueryPort.class);
        JgitkinsException expected = mock(JgitkinsException.class);
        when(queryPort.findByName(OrganizeName.from("alice"))).thenThrow(expected);
        assertThatThrownBy(() -> new OrganizationNameUniquenessAclAdapter(queryPort).isAvailableForUsername("alice"))
                .isSameAs(expected);
    }
}
