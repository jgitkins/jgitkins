package io.jgitkins.server.domain.model;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import io.jgitkins.server.domain.exception.UserAlreadyActivatedException;
import io.jgitkins.server.domain.model.vo.Username;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final UserAuthority authority;
    private final UserStatus status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User create(String username,
                              String email,
                              String displayName,
                              String avatarUrl) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        LocalDateTime now = LocalDateTime.now();
        return new User(null,
                        username.trim(),
                        normalize(email),
                        normalize(displayName),
                        normalize(avatarUrl),
                        UserAuthority.USER,
                        UserStatus.ACTIVE,
                        now,
                        now,
                        now);
    }

    public static User createWithStatus(String username,
                                        String email,
                                        String displayName,
                                        String avatarUrl,
                                        UserStatus status) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        LocalDateTime now = LocalDateTime.now();
        return new User(null,
                        username.trim(),
                        normalize(email),
                        normalize(displayName),
                        normalize(avatarUrl),
                        UserAuthority.USER,
                        status != null ? status : UserStatus.ACTIVE,
                        now,
                        now,
                        now);
    }

    public User withId(Long id) {
        return new User(id,
                        username,
                        email,
                        displayName,
                        avatarUrl,
                        authority,
                        status,
                        lastLoginAt,
                        createdAt,
                        updatedAt);
    }

    public User updateProfile(String email, String displayName, String avatarUrl) {
        return new User(id,
                        username,
                        normalize(email),
                        normalize(displayName),
                        normalize(avatarUrl),
                        authority,
                        status,
                        lastLoginAt,
                        createdAt,
                        LocalDateTime.now());
    }

    public User activateWithUsername(Username username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        if (status != UserStatus.PENDING) {
            throw new UserAlreadyActivatedException();
        }
        return new User(id,
                        username.getValue(),
                        email,
                        displayName,
                        avatarUrl,
                        authority,
                        UserStatus.ACTIVE,
                        lastLoginAt,
                        createdAt,
                        LocalDateTime.now());
    }

    public User touchLogin(LocalDateTime loginAt) {
        LocalDateTime when = loginAt != null ? loginAt : LocalDateTime.now();
        return new User(id,
                        username,
                        email,
                        displayName,
                        avatarUrl,
                        authority,
                        status,
                        when,
                        createdAt,
                        updatedAt);
    }

    public static User rehydrate(Long id,
                                 String username,
                                 String email,
                                 String displayName,
                                 String avatarUrl,
                                 UserAuthority authority,
                                 UserStatus status,
                                 LocalDateTime lastLoginAt,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        return new User(id,
                        username,
                        email,
                        displayName,
                        avatarUrl,
                        authority,
                        status,
                        lastLoginAt,
                        createdAt,
                        updatedAt);
    }

    public static User rehydrate(Long id,
                                 String username,
                                 String email,
                                 String displayName,
                                 String avatarUrl,
                                 UserStatus status,
                                 LocalDateTime lastLoginAt,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        return new User(id,
                        username,
                        email,
                        displayName,
                        avatarUrl,
                        UserAuthority.USER,
                        status,
                        lastLoginAt,
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
