package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.identity.access.application.contract.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.infrastructure.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenCodec {
    private final JwtProperties jwtProperties;

    public String issueToken(Long userId, List<String> roles) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getTtlSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Optional<JwtAuthenticationResult> verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey()).build()
                    .parseSignedClaims(token).getPayload();
            Long userId = parseSubject(claims.getSubject());
            if (userId == null) return Optional.empty();
            Object rawRoles = claims.get("roles");
            if (!claims.containsKey("roles")) return Optional.of(new JwtAuthenticationResult(userId, List.of()));
            if (!(rawRoles instanceof List<?> roles)) return Optional.empty();
            for (Object role : roles) {
                if (!(role instanceof String)) return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            List<String> stringRoles = (List<String>) roles;
            return Optional.of(new JwtAuthenticationResult(userId, stringRoles));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Turns a token's subject into a user id, strictly.
     *
     * <p><strong>Moved here from the identity inbound adapter's {@code RequesterUserIdResolver} in
     * task 2.88a, before that resolver is deleted.</strong> Until this moved, the only place these
     * four rejections happened was inside a class scheduled for removal, and every other consumer of
     * a verified token got {@code Long.valueOf} — which accepts all four of them.
     *
     * <p>What {@code Long.valueOf} lets through, and why each one matters:
     *
     * <ul>
     *   <li><strong>{@code "0"}</strong> — parses to 0. No user has id zero, so a subject of zero is
     *       either a placeholder that leaked from somewhere or a truncated value. Accepting it means
     *       authenticating as a row that cannot exist.</li>
     *   <li><strong>{@code "-1"}</strong> — parses fine. Same reasoning; ids are positive.</li>
     *   <li><strong>{@code "\u0665"}</strong> (Arabic-Indic five) — <em>parses to 5</em>.
     *       {@code Long.parseLong} delegates to {@code Character.digit}, which accepts Devanagari,
     *       Arabic-Indic and every other Unicode decimal digit. An id that depends on the script it
     *       was written in is not an id.</li>
     *   <li><strong>{@code "+1"}</strong> — parses to 1. A signed form is a second spelling of the
     *       same id, and two spellings of one identity is one more than an identity should have.</li>
     * </ul>
     *
     * <p>Returns null rather than throwing, matching this method's surroundings: {@code verify}
     * answers {@code Optional} and {@code JwtAuthenticationFilter} turns empty into 401. The
     * absent-versus-malformed split the resolver drew belongs at the controller boundary, where a
     * missing principal means an anonymous request. Here a token was presented and its signature
     * checked out, so anything unusable in it is a broken credential, not an absent one.
     *
     * <p>Leading zeros ({@code "007"}) are accepted, deliberately unchanged. {@code issueToken}
     * writes {@code String.valueOf}, so this codec never produces them, and the resolver being
     * replaced accepted them too. Tightening it here would make step 1 something other than a move,
     * and a migration that also changes behaviour is a migration nobody can revert with confidence.
     */
    private static Long parseSubject(String subject) {
        if (subject == null || subject.isEmpty()) {
            return null;
        }
        for (int index = 0; index < subject.length(); index++) {
            char character = subject.charAt(index);
            // Explicitly ASCII. Character.isDigit is the trap, not the fix.
            if (character < '0' || character > '9') {
                return null;
            }
        }
        long parsed;
        try {
            parsed = Long.parseLong(subject);
        } catch (NumberFormatException overflow) {
            // All digits but longer than a long. Not a usable id.
            return null;
        }
        return parsed > 0 ? parsed : null;
    }

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
