package io.jgitkins.server.application.dto.command;

public record UserLoginOrSignUpCommand(
        String providerName,
        String providerSub,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {
}
