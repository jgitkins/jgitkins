package io.jgitkins.web.application.dto;

public record OrganizeCreateRequest(
		String name,
		Long ownerId,
		String description
) {
}
