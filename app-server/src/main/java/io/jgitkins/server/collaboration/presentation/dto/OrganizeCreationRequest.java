package io.jgitkins.server.collaboration.presentation.dto;

public record OrganizeCreationRequest(
        String name,
        Long ownerId,
        String description
) {
}
