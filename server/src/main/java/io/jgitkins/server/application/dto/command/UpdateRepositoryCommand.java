package io.jgitkins.server.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRepositoryCommand {
    private String name;
    private String path;
    private String defaultBranch;
    private String visibility;
    private Long ownerId;
    private String description;
    private String credentialId;
    private LocalDateTime lastSyncedAt;
}
