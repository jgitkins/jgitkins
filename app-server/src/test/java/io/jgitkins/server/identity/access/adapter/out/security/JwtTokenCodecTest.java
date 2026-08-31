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

    /**
     * The four subjects {@code Long.valueOf} accepts and a user id must not be.
     *
     * <p>Task 2.88a moved these rejections here from the identity inbound adapter's
     * {@code RequesterUserIdResolver}, which task 2.88b deletes. Without this test the move is
     * unverified, and deleting the resolver would take the only enforcement of these four rules with
     * it -- silently, because every one of them parses.
     *
     * <p>The case set was carried over wholesale from {@code RequesterUserIdResolverTest} when 2.88b
     * deleted that resolver. Porting it rather than writing a smaller one on the way past matters:
     * that test asserted things this one did not, including that leading zeros are accepted, and a
     * migration that quietly narrows coverage is how a rule stops being enforced without anyone
     * choosing to stop enforcing it.
     */
    @org.junit.jupiter.params.ParameterizedTest(name = "subject \"{0}\" is refused")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            // Non-positive. Parse cleanly, name no row that can exist.
            "0", "00", "000", "0000000000", "-0", "-1",
            // Non-ASCII digits. Character.digit accepts these, so Long.parseLong reads them as
            // numbers -- an id that depends on the script it was written in is not an id.
            "\u0665",
            // Extra spellings of one id.
            "+1",
            // Whitespace. String.isBlank treats every Unicode space as blank, so a mangled token
            // padded with one would have been reported as an absent principal rather than a broken
            // credential. The check is ASCII-explicit for that reason.
            " ", "\t", "\n", "\r", "   \t\n\r  ", "1 ", " 1", "\t42", "42 ",
            // Not digit strings, but plausible enough that a lenient parser might take them.
            "abc", "4a2", "42.0", "42L", "4_2", "1e3",
            // All digits, wider than a long.
            "99999999999999999999", "9999999999999999999999"
    })
    void verifyRefusesSubjectsThatAreNotUserIds(String subject) {
        JwtProperties p = properties(900);
        JwtTokenCodec codec = new JwtTokenCodec(p);
        String token = Jwts.builder().subject(subject)
                .signWith(Keys.hmacShaKeyFor(p.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertTrue(codec.verify(token).isEmpty(),
                () -> "subject \"" + subject + "\" was accepted; Long.valueOf would have parsed it");
    }

    @Test
    void verifyStillAcceptsAnOrdinarySubject() {
        // The control. Without it, a parser that rejects everything would pass the test above and
        // lock every user out, which is a worse outcome than the one being prevented.
        JwtProperties p = properties(900);
        JwtTokenCodec codec = new JwtTokenCodec(p);
        String token = Jwts.builder().subject("7")
                .signWith(Keys.hmacShaKeyFor(p.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(7L, codec.verify(token).orElseThrow().userId());
    }

    private JwtProperties properties(long ttl) {
        JwtProperties p = new JwtProperties();
        p.setSecret("01234567890123456789012345678901");
        p.setTtlSeconds(ttl);
        return p;
    }
}
