package io.jgitkins.web.application.contract;

public record OrganizeCreateRequest(
		String name,
		Long ownerId,
		String description
) {
}
