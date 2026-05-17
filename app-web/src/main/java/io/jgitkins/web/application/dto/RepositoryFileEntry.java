package io.jgitkins.web.application.dto;

public record RepositoryFileEntry(
		String id,
		String name,
		String path,
		String type,
		String mode,
		Long size
) {
}
