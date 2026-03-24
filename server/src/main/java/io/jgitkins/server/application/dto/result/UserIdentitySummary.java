package io.jgitkins.server.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserIdentitySummary {
    private final String providerName;
    private final String providerSub;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final String avatarUrl;
}
