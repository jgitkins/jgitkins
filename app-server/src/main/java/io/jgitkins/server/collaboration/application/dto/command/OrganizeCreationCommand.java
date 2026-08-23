package io.jgitkins.server.collaboration.application.dto.command;

public record OrganizeCreationCommand(
        String name,
        String description,
        Long requesterUserId
) {
}

