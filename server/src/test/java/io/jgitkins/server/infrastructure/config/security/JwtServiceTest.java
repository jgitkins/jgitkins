package io.jgitkins.server.infrastructure.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jgitkins.server.infrastructure.adapter.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void issueToken_andParseClaims_containsSubjectAndRoles() {
        JwtService jwtService = new JwtService(jwtProperties(3600));

        String token = jwtService.issueToken(100L, List.of("ROLE_USER", "ROLE_ADMIN"));
        Claims claims = jwtService.parseClaims(token);

        assertNotNull(token);
        assertEquals("100", claims.getSubject());
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), claims.get("roles", List.class));
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void issueToken_throwsWhenUserIdMissing() {
        JwtService jwtService = new JwtService(jwtProperties(3600));

        assertThrows(IllegalArgumentException.class, () -> jwtService.issueToken(null, List.of("ROLE_USER")));
    }

    @Test
    void isValid_returnsFalseForMalformedToken() {
        JwtService jwtService = new JwtService(jwtProperties(3600));

        assertFalse(jwtService.isValid("not-a-jwt-token"));
    }

    @Test
    void issueToken_throwsWhenSecretMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setTtlSeconds(3600);
        JwtService jwtService = new JwtService(properties);

        assertThrows(IllegalStateException.class, () -> jwtService.issueToken(1L, List.of("ROLE_USER")));
    }

    private JwtProperties jwtProperties(long ttlSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setTtlSeconds(ttlSeconds);
        return properties;
    }
}
