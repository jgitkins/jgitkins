package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAdminSummary {
    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String status;
    private final LocalDateTime lastLoginAt;
}
