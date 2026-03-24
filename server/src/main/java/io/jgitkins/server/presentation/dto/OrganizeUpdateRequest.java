package io.jgitkins.server.presentation.dto;

public record OrganizeUpdateRequest(
        String name,
        Long ownerId,
        String description
) {
}
