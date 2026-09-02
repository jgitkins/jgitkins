package io.jgitkins.web.application.contract;

public record OrganizeCreateResult(
		OrganizeSummary organize,
		String errorMessage
) {
}
