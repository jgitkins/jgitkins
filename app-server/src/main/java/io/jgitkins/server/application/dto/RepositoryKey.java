package io.jgitkins.server.application.dto;

import org.springframework.util.StringUtils;

public record RepositoryKey(String namespace, String repoName) {

	public static RepositoryKey fromPath(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = trimSlashes(value);
		if (trimmed.endsWith(".git")) {
			trimmed = trimmed.substring(0, trimmed.length() - 4);
		}
		String[] parts = trimmed.split("/");
		if (parts.length < 2) {
			return null;
		}
		String repoName = parts[parts.length - 1];
		String namespace = String.join("/", java.util.Arrays.copyOf(parts, parts.length - 1));
		return new RepositoryKey(namespace, repoName);
	}

	private static String trimSlashes(String value) {
		return value.replaceAll("^/+", "").replaceAll("/+$", "");
	}
}
