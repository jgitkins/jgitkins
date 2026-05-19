package io.jgitkins.server.collaboration.presentation.dto;

public record OrganizeUpdateRequest(
        String name,
        Long ownerId,
        String description
) {
}
