package io.jgitkins.server.identity.access.application.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UsernameAllocatorTest {
    @Test
    void allocateUniqueUsername_returnsBaseWhenAvailable() {
        UserQueryPort userQueryPort = mock(UserQueryPort.class);
        OrganizeQueryPort organizePort = mock(OrganizeQueryPort.class);
        when(userQueryPort.existsByUsername(anyString())).thenReturn(false);
        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        UsernameAllocator allocator = new UsernameAllocator(userQueryPort, organizePort);
        assertEquals("base", allocator.allocateUniqueUsername("base", "provider-sub"));
    }

    @Test
    void allocateUniqueUsername_fallsBackToProviderSuffixWhenBaseTaken() {
        UserQueryPort userQueryPort = mock(UserQueryPort.class);
        OrganizeQueryPort organizePort = mock(OrganizeQueryPort.class);
        when(userQueryPort.existsByUsername(anyString())).thenAnswer(invocation -> "base".equals(invocation.getArgument(0)));
        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        UsernameAllocator allocator = new UsernameAllocator(userQueryPort, organizePort);
        assertEquals("base-123456", allocator.allocateUniqueUsername("base", "ABCDEF123456"));
    }
}
