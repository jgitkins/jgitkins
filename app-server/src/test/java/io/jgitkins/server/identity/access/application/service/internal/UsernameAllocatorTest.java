package io.jgitkins.server.identity.access.application.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.port.out.OrganizationNameUniquenessPort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import org.junit.jupiter.api.Test;

class UsernameAllocatorTest {
    @Test
    void allocateUniqueUsername_returnsBaseWhenAvailable() {
        UserQueryPort userQueryPort = mock(UserQueryPort.class);
        OrganizationNameUniquenessPort organizationNameUniquenessPort = mock(OrganizationNameUniquenessPort.class);
        when(userQueryPort.existsByUsername(anyString())).thenReturn(false);
        when(organizationNameUniquenessPort.isAvailableForUsername(anyString())).thenReturn(true);
        UsernameAllocator allocator = new UsernameAllocator(userQueryPort, organizationNameUniquenessPort);
        assertEquals("base", allocator.allocateUniqueUsername("base", "provider-sub"));
    }

    @Test
    void allocateUniqueUsername_fallsBackToProviderSuffixWhenBaseTaken() {
        UserQueryPort userQueryPort = mock(UserQueryPort.class);
        OrganizationNameUniquenessPort organizationNameUniquenessPort = mock(OrganizationNameUniquenessPort.class);
        when(userQueryPort.existsByUsername(anyString())).thenAnswer(invocation -> "base".equals(invocation.getArgument(0)));
        when(organizationNameUniquenessPort.isAvailableForUsername(anyString())).thenReturn(true);
        UsernameAllocator allocator = new UsernameAllocator(userQueryPort, organizationNameUniquenessPort);
        assertEquals("base-123456", allocator.allocateUniqueUsername("base", "ABCDEF123456"));
    }
}
