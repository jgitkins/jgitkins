package io.jgitkins.server.collaboration.application.contract;

public record OrganizeCreationCommand(
        String name,
        String description,
        Long requesterUserId
) {
}

