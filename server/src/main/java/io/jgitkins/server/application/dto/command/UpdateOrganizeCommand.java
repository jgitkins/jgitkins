package io.jgitkins.server.application.dto.command;

public record UpdateOrganizeCommand(
        String name,
        Long ownerId,
        String description
) {
}
