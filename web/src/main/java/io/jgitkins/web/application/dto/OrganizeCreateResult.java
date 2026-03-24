package io.jgitkins.web.application.dto;

public record OrganizeCreateResult(
		OrganizeSummary organize,
		String errorMessage
) {
}
