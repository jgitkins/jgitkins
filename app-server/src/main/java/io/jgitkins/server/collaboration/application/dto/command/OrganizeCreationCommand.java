package io.jgitkins.server.collaboration.application.dto.command;

public record OrganizeCreationCommand(
        String name,
        Long ownerId,
        String description
) {
}

