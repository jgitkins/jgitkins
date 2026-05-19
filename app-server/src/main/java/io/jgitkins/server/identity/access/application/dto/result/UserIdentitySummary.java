package io.jgitkins.server.identity.access.application.dto.result;

public record UserIdentitySummary(
        String providerName,
        String providerSub,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {
}
