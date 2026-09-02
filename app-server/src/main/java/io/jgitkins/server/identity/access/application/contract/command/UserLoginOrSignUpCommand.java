package io.jgitkins.server.identity.access.application.contract.command;

public record UserLoginOrSignUpCommand(
        String providerName,
        String providerSub,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {
}
