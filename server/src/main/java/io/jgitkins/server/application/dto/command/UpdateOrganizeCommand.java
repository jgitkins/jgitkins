package io.jgitkins.server.application.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateOrganizeCommand {
    private final String name;
    private final Long ownerId;
    private final String description;
}
