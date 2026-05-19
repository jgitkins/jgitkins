package io.jgitkins.server.collaboration.application.dto.command;

public record UpdateOrganizeCommand(
        String name,
        Long ownerId,
        String description
) {
}
