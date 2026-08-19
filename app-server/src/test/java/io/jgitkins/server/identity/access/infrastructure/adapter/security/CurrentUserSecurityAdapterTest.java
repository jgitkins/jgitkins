package io.jgitkins.server.identity.access.infrastructure.adapter.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

class CurrentUserSecurityAdapterTest {
    private final CurrentUserSecurityAdapter adapter = new CurrentUserSecurityAdapter();

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void resolveCurrentUserId_returnsEmptyWithoutAuthentication() {
        assertEquals(Optional.empty(), adapter.resolveCurrentUserId());
    }

    @Test
    void resolveCurrentUserId_returnsEmptyForUnauthenticatedAuthentication() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("7", "");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertEquals(Optional.empty(), adapter.resolveCurrentUserId());
    }

    @Test
    void resolveCurrentUserId_returnsNumericSubject() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("7", "", List.of()));
        assertEquals(Optional.of(7L), adapter.resolveCurrentUserId());
    }

    @Test
    void resolveCurrentUserId_returnsEmptyForInvalidSubject() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-number", "", List.of()));
        assertEquals(Optional.empty(), adapter.resolveCurrentUserId());
    }
}
