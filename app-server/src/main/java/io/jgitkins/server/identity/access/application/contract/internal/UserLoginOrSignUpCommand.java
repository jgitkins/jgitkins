package io.jgitkins.server.identity.access.application.contract.internal;

public record UserLoginOrSignUpCommand(
        String providerName,
        String providerSub,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {
}
