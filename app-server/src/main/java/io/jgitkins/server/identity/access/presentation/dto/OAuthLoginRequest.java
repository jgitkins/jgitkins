package io.jgitkins.server.identity.access.presentation.dto;

public record OAuthLoginRequest(
        String provider,
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String avatarUrl
) {
}
