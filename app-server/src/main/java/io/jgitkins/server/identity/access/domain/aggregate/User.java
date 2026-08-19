package io.jgitkins.server.identity.access.domain.aggregate;

import io.jgitkins.server.identity.access.domain.exception.UserAlreadyActivatedException;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.identity.access.domain.vo.Username;
import java.time.LocalDateTime;

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

    private User(Long id, String username, String email, String displayName, String avatarUrl,
                 UserAuthority authority, UserStatus status, LocalDateTime lastLoginAt,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.authority = authority;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String username, String email, String displayName, String avatarUrl) {
        requireUsername(username);
        LocalDateTime now = LocalDateTime.now();
        return new User(null, username.trim(), normalize(email), normalize(displayName), normalize(avatarUrl),
                UserAuthority.USER, UserStatus.ACTIVE, now, now, now);
    }

    public static User createWithStatus(String username, String email, String displayName, String avatarUrl, UserStatus status) {
        requireUsername(username);
        LocalDateTime now = LocalDateTime.now();
        return new User(null, username.trim(), normalize(email), normalize(displayName), normalize(avatarUrl),
                UserAuthority.USER, status != null ? status : UserStatus.ACTIVE, now, now, now);
    }

    public User withId(Long id) { return copy(id, username, email, displayName, avatarUrl, authority, status, lastLoginAt, createdAt, updatedAt); }

    public User updateProfile(String email, String displayName, String avatarUrl) {
        return copy(id, username, normalize(email), normalize(displayName), normalize(avatarUrl), authority, status,
                lastLoginAt, createdAt, LocalDateTime.now());
    }

    public User activateWithUsername(Username username) {
        if (username == null) throw new IllegalArgumentException("Username is required");
        if (status != UserStatus.PENDING) throw new UserAlreadyActivatedException();
        return copy(id, username.getValue(), email, displayName, avatarUrl, authority, UserStatus.ACTIVE,
                lastLoginAt, createdAt, LocalDateTime.now());
    }

    public User touchLogin(LocalDateTime loginAt) {
        return copy(id, username, email, displayName, avatarUrl, authority, status,
                loginAt != null ? loginAt : LocalDateTime.now(), createdAt, updatedAt);
    }

    public static User rehydrate(Long id, String username, String email, String displayName, String avatarUrl,
                                 UserAuthority authority, UserStatus status, LocalDateTime lastLoginAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        return copy(id, username, email, displayName, avatarUrl, authority, status, lastLoginAt, createdAt, updatedAt);
    }

    public static User rehydrate(Long id, String username, String email, String displayName, String avatarUrl,
                                 UserStatus status, LocalDateTime lastLoginAt, LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
        return copy(id, username, email, displayName, avatarUrl, UserAuthority.USER, status,
                lastLoginAt, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public UserAuthority getAuthority() { return authority; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    private static User copy(Long id, String username, String email, String displayName, String avatarUrl,
                             UserAuthority authority, UserStatus status, LocalDateTime lastLoginAt,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, username, email, displayName, avatarUrl, authority, status,
                lastLoginAt, createdAt, updatedAt);
    }

    private static void requireUsername(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
