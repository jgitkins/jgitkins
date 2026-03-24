package io.jgitkins.web.application.dto;

import java.util.List;

public record NamespaceSummary(
		boolean organization,
		String name,
		String description,
		List<RepositorySummary> repositories,
		List<OrganizeMemberSummary> members,
		String errorMessage) {
}
