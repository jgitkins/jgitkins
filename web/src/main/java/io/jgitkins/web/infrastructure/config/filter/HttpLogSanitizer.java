package io.jgitkins.web.infrastructure.config.filter;

import java.util.regex.Pattern;

final class HttpLogSanitizer {

	private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
			"(?i)(\\\"(?:password|token|authorization|secret|accessToken|refreshToken|appToken)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")"
	);
	private static final Pattern FORM_SECRET_PATTERN = Pattern.compile(
			"(?i)((?:password|token|authorization|secret|access_token|refresh_token|app_token)=)([^&\\s]+)"
	);

	private HttpLogSanitizer() {
	}

	static String sanitizeAndTruncate(String body, int maxLength) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String masked = maskSecrets(body);
		if (masked.length() <= maxLength) {
			return masked;
		}
		return masked.substring(0, maxLength) + " ...";
	}

	private static String maskSecrets(String body) {
		String masked = JSON_SECRET_PATTERN.matcher(body).replaceAll("$1***$3");
		return FORM_SECRET_PATTERN.matcher(masked).replaceAll("$1***");
	}
}
