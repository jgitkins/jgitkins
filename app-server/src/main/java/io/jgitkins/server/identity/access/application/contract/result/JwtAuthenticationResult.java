package io.jgitkins.server.identity.access.application.contract.result;

import java.util.List;
import java.util.Objects;

public record JwtAuthenticationResult(Long userId, List<String> roles) {
    public JwtAuthenticationResult {
        userId = Objects.requireNonNull(userId, "userId is required");
        roles = List.copyOf(Objects.requireNonNull(roles, "roles are required"));
    }
}
