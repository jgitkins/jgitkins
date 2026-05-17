package io.jgitkins.server.presentation.dto;

public record OrganizeCreationRequest(
        String name,
        Long ownerId,
        String description
) {
}
