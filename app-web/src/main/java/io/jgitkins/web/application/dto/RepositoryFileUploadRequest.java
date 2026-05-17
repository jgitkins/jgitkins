package io.jgitkins.web.application.dto;

public record RepositoryFileUploadRequest(
		Long repositoryId,
		String branch,
		String path,
		String message,
		String originalFilename,
		String contentType,
		byte[] content
) {
}
