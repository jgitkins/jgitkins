package io.jgitkins.web.application.dto;

public record RepositoryCreateRequest(
		String repoName,
		String mainBranch,
		String username,
		String email,
		boolean readme,
		String message,
		String ownerType,
		Long organizeId,
		String visibility,
		String description,
		String credentialId
) {
}
