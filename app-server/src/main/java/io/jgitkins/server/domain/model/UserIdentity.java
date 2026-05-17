package io.jgitkins.server.domain.model;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserIdentity {

    private final Long id;
    private final Long userId;
    private final String providerName;
    private final String providerSub;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final String avatarUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static UserIdentity create(Long userId,
                                      String providerName,
                                      String providerSub,
                                      String email,
                                      boolean emailVerified,
                                      String name,
                                      String avatarUrl) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("providerName is required");
        }
        if (providerSub == null || providerSub.isBlank()) {
            throw new IllegalArgumentException("providerSub is required");
        }
        LocalDateTime now = LocalDateTime.now();
        return new UserIdentity(null,
                                userId,
                                providerName.trim(),
                                providerSub.trim(),
                                normalize(email),
                                emailVerified,
                                normalize(name),
                                normalize(avatarUrl),
                                now,
                                now);
    }

    public UserIdentity withId(Long id) {
        return new UserIdentity(id,
                                userId,
                                providerName,
                                providerSub,
                                email,
                                emailVerified,
                                name,
                                avatarUrl,
                                createdAt,
                                updatedAt);
    }

    public UserIdentity updateProfile(String email,
                                      boolean emailVerified,
                                      String name,
                                      String avatarUrl) {
        return new UserIdentity(id,
                                userId,
                                providerName,
                                providerSub,
                                normalize(email),
                                emailVerified,
                                normalize(name),
                                normalize(avatarUrl),
                                createdAt,
                                LocalDateTime.now());
    }

    public static UserIdentity rehydrate(Long id,
                                         Long userId,
                                         String providerName,
                                         String providerSub,
                                         String email,
                                         boolean emailVerified,
                                         String name,
                                         String avatarUrl,
                                         LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        return new UserIdentity(id,
                                userId,
                                providerName,
                                providerSub,
                                email,
                                emailVerified,
                                name,
                                avatarUrl,
                                createdAt,
                                updatedAt);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
