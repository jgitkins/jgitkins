package io.jgitkins.server.application.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrganizeCreationResult {
    private final Long id;
    private final String name;
    private final String description;
    private final Long ownerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
