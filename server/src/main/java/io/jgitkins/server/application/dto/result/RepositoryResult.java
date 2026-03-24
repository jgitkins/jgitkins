package io.jgitkins.server.application.dto.result;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class RepositoryResult {
    private final Long id;
    private final String ownerType;
    private final String name;
    private final String path;
    private final String defaultBranch;
    private final String visibility;
    private final String description;
    private final Long ownerId;
    private final String credentialId;
    private final String clonePath;
    private final String cloneUrl;
    private final boolean requiresInitialContent;
    private final LocalDateTime lastSyncedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
