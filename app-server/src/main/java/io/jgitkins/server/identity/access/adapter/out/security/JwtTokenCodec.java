package io.jgitkins.server.identity.access.adapter.out.security;

import io.jgitkins.server.identity.access.application.dto.result.JwtAuthenticationResult;
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
            String subject = claims.getSubject();
            if (subject == null) return Optional.empty();
            Long userId;
            try {
                userId = Long.valueOf(subject);
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
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

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
