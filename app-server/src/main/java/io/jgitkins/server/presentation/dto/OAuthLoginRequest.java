package io.jgitkins.server.presentation.dto;

public record OAuthLoginRequest(
        String provider,
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String avatarUrl
) {
}
