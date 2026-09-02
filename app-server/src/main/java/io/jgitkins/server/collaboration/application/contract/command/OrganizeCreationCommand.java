package io.jgitkins.server.collaboration.application.contract.command;

public record OrganizeCreationCommand(
        String name,
        String description,
        Long requesterUserId
) {
}

