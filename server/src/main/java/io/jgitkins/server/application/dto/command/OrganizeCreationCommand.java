package io.jgitkins.server.application.dto.command;

public record OrganizeCreationCommand(
        String name,
        Long ownerId,
        String description
) {
}
