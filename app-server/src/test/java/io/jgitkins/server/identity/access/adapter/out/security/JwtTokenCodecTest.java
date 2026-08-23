package io.jgitkins.server.identity.access.adapter.out.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jgitkins.server.identity.access.infrastructure.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenCodecTest {
    @Test
    void issueAndVerifyPreservesSubjectRolesAndTtl() {
        JwtTokenCodec codec = new JwtTokenCodec(properties(900));
        String token = codec.issueToken(100L, List.of("ROLE_USER", "ROLE_ADMIN"));
        var result = codec.verify(token);
        assertTrue(result.isPresent());
        assertEquals(100L, result.get().userId());
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), result.get().roles());
    }

    @Test
    void invalidSubjectsAndRolesFailClosed() {
        JwtTokenCodec codec = new JwtTokenCodec(properties(900));
        assertTrue(codec.verify("not-a-token").isEmpty());
        String token = Jwts.builder().subject("not-numeric").claim("roles", List.of("ROLE_USER"))
                .signWith(Keys.hmacShaKeyFor(properties(900).getSecret().getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(codec.verify(token).isEmpty());
    }

    @Test
    void verifyRejectsNullSubjectExpiredSignatureInvalidAndInvalidKeyTokens() {
        JwtProperties p = properties(900);
        JwtTokenCodec codec = new JwtTokenCodec(p);
        String key = p.getSecret();
        String expired = Jwts.builder().subject("7").expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
        String wrongSignature = Jwts.builder().subject("7")
                .signWith(Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8))).compact();
        JwtProperties invalidKey = properties(900);
        invalidKey.setSecret("too-short");

        assertTrue(codec.verify(Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact()).isEmpty());
        assertTrue(codec.verify(expired).isEmpty());
        assertTrue(codec.verify(wrongSignature).isEmpty());
        assertTrue(new JwtTokenCodec(invalidKey).verify("anything").isEmpty());
    }

    @Test
    void verifyRejectsMalformedNullAndMixedRolesButAllowsMissingRolesAsEmpty() {
        JwtProperties p = properties(900);
        JwtTokenCodec codec = new JwtTokenCodec(p);
        var signingKey = Keys.hmacShaKeyFor(p.getSecret().getBytes(StandardCharsets.UTF_8));
        String malformed = Jwts.builder().subject("7").claim("roles", "ROLE_USER").signWith(signingKey).compact();
        String mixed = Jwts.builder().subject("7").claim("roles", List.of("ROLE_USER", 42)).signWith(signingKey).compact();
        String nullRole = Jwts.builder().subject("7").claim("roles", java.util.Arrays.asList("ROLE_USER", null)).signWith(signingKey).compact();
        String missing = Jwts.builder().subject("7").signWith(signingKey).compact();

        assertTrue(codec.verify(malformed).isEmpty());
        assertTrue(codec.verify(mixed).isEmpty());
        assertTrue(codec.verify(nullRole).isEmpty());
        assertEquals(List.of(), codec.verify(missing).orElseThrow().roles());
    }

    @Test
    void missingRolesAreEmptyAndNullUserOrSecretPreserveExceptions() {
        JwtProperties p = properties(900);
        JwtTokenCodec codec = new JwtTokenCodec(p);
        assertThrows(IllegalArgumentException.class, () -> codec.issueToken(null, List.of()));
        p.setSecret(null);
        assertThrows(IllegalStateException.class, () -> codec.issueToken(1L, List.of()));
        assertNotNull(codec);
    }

    private JwtProperties properties(long ttl) {
        JwtProperties p = new JwtProperties();
        p.setSecret("01234567890123456789012345678901");
        p.setTtlSeconds(ttl);
        return p;
    }
}
