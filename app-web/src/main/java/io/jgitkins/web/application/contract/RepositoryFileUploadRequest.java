package io.jgitkins.web.application.contract;

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
