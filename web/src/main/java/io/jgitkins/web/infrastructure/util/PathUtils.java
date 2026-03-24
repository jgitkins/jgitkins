package io.jgitkins.web.infrastructure.util;

import io.jgitkins.web.application.model.RepositoryKey;
import org.springframework.util.StringUtils;

public final class PathUtils {

	private PathUtils() {
	}

	public static String trimSlashes(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		String trimmed = StringUtils.trimLeadingCharacter(value, '/');
		return StringUtils.trimTrailingCharacter(trimmed, '/');
	}

	public static RepositoryKey parseRepositoryKey(String value) {
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

	public static RepositoryKey resolveRepositoryKey(String clonePath, String path) {
		RepositoryKey key = parseRepositoryKey(clonePath);
		if (key != null) {
			return key;
		}
		return parseRepositoryKey(path);
	}

	public static String lastSegment(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		String trimmed = StringUtils.trimTrailingCharacter(value, '/');
		int index = trimmed.lastIndexOf('/');
		if (index < 0) {
			return trimmed;
		}
		return trimmed.substring(index + 1);
	}
}
