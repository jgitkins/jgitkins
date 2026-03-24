package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAdminDetail {
    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final String status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<UserIdentitySummary> identities;
}
