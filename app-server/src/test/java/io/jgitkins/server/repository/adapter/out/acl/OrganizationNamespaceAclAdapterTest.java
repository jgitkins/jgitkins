package io.jgitkins.server.repository.adapter.out.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A namespace that cannot be an organization name is "no organization", not an error.
 *
 * <p>The argument is a path segment a client typed. {@code OrganizeName.from} throws on anything
 * outside {@code [A-Za-z0-9_-]+}, so without the {@code isValid} check every unusable namespace
 * reaches the caller as an exception rather than an empty result -- a 500 where a 404 belongs, for a
 * URL anyone can type. That is the shape 529ff34 had to fix elsewhere, and it is the one real
 * behavioural risk in routing the two repository persistence adapters through this port: they used to
 * run a raw {@code where NAME = ?} that simply matched nothing.
 *
 * <p>These tests exist because removing the guard broke nothing. The suite passed with it deleted,
 * measured, which meant the contract this refactor depends on was asserted nowhere.
 */
class OrganizationNamespaceAclAdapterTest {

    private final OrganizeQueryPort delegate = mock(OrganizeQueryPort.class);
    private final OrganizationNamespaceAclAdapter adapter = new OrganizationNamespaceAclAdapter(delegate);

    @ParameterizedTest
    @ValueSource(strings = {
            "has space",
            "dot.separated",
            "slash/inside",
            "colon:inside",
            "percent%20encoded",
            "  ",
            "",
            "한글",
            "semi;colon",
            "quote'mark"})
    void aNamespaceThatCannotBeAnOrganizationNameIsEmptyRatherThanAnException(String namespace) {
        assertThatCode(() -> adapter.findOrganizationIdByName(namespace))
                .as("a client can type any of these into a URL; throwing turns the 404 the caller "
                        + "expects into a 500")
                .doesNotThrowAnyException();

        assertThat(adapter.findOrganizationIdByName(namespace)).isEmpty();
        verify(delegate, never()).findByName(any());
    }

    @Test
    void nullIsEmptyToo() {
        assertThat(adapter.findOrganizationIdByName(null))
                .as("OrganizeName.from throws on null before isValid would even be consulted")
                .isEmpty();
        verify(delegate, never()).findByName(any());
    }

    @Test
    void aUsableNameIsHandedToTheOwningContext() {
        Organize organize = mock(Organize.class);
        when(organize.getId()).thenReturn(OrganizeId.of(77L));
        when(delegate.findByName(OrganizeName.from("acme-corp"))).thenReturn(Optional.of(organize));

        assertThat(adapter.findOrganizationIdByName("acme-corp")).contains(77L);
    }

    @Test
    void aUsableNameWithNoOrganizationIsEmpty() {
        when(delegate.findByName(any())).thenReturn(Optional.empty());

        assertThat(adapter.findOrganizationIdByName("no-such-org"))
                .as("the guard must not swallow the difference between an unusable name and a name "
                        + "nobody has taken -- both are empty, and the delegate decides the second")
                .isEmpty();
        verify(delegate).findByName(OrganizeName.from("no-such-org"));
    }

    @Test
    void onlyIdsCrossTheBoundary() {
        Organize organize = mock(Organize.class);
        when(organize.getId()).thenReturn(OrganizeId.of(5L));
        when(delegate.findByName(any())).thenReturn(Optional.of(organize));

        Optional<Long> result = adapter.findOrganizationIdByName("acme");

        assertThat(result).containsInstanceOf(Long.class);
        verify(organize).getId();
        // Nothing else is read off the aggregate. The port returns an id because that is all the
        // repository context filters by, and an ACL that returned the aggregate would hand this
        // context invariants collaboration enforces.
        verify(organize, never()).getName();
    }
}
