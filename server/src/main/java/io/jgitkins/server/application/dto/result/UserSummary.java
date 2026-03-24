package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSummary {
    private final Long id;
    private final String username;
    private final String displayName;
    private final String avatarUrl;
    private final LocalDateTime createdAt;
}
