package io.jgitkins.server.collaboration.adapter.in.rest.dto.request;

public record OrganizeUpdateRequest(
        String name,
        Long ownerId,
        String description
) {
}
