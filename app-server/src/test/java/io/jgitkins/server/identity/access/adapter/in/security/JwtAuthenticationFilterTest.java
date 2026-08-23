package io.jgitkins.server.identity.access.adapter.in.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.common.infrastructure.config.security.handler.ApiAnauthorizeHandler;
import io.jgitkins.server.identity.access.application.dto.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationFilterTest {
    @Test
    void excludesExactPathsAndPassesMissingBearer() throws Exception {
        JwtAuthService service = mock(JwtAuthService.class);
        ApiAnauthorizeHandler handler = mock(ApiAnauthorizeHandler.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service, handler);
        MockHttpServletRequest excluded = new MockHttpServletRequest("GET", "/login/google");
        assertTrue(filter.shouldNotFilter(excluded));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verifyNoInteractions(service, handler);
        assertNotNull(chain.getRequest());
    }

    @Test
    void validNumericSubjectMapsRolesAndInvalidTokenUnauthorized() throws Exception {
        JwtAuthService service = mock(JwtAuthService.class);
        ApiAnauthorizeHandler handler = mock(ApiAnauthorizeHandler.class);
        when(service.authenticate("valid")).thenReturn(Optional.of(new JwtAuthenticationResult(42L, List.of("ROLE_USER"))));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(service, handler);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer valid");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertEquals("42", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        SecurityContextHolder.clearContext();
        when(service.authenticate("bad")).thenReturn(Optional.empty());
        request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer bad");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        verify(handler).commence(any(), any(), isNull());
    }
}
