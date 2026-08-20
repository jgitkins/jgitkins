package io.jgitkins.server.identity.access.adapter.in.rest.dto.request;

public record OAuthLoginRequest(
        String provider,
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String avatarUrl
) {
}
