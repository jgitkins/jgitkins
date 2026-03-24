package io.jgitkins.web.infrastructure.config.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HttpLogSanitizerTest {

	@Test
	void sanitizeAndTruncate_masksJsonSecrets() {
		String body = "{\"token\":\"abc\",\"password\":\"pw\",\"name\":\"ok\"}";
		String sanitized = HttpLogSanitizer.sanitizeAndTruncate(body, 500);

		assertTrue(sanitized.contains("\"token\":\"***\""));
		assertTrue(sanitized.contains("\"password\":\"***\""));
		assertTrue(sanitized.contains("\"name\":\"ok\""));
		assertFalse(sanitized.contains("abc"));
		assertFalse(sanitized.contains("pw"));
	}

	@Test
	void sanitizeAndTruncate_masksFormSecrets() {
		String body = "name=x&token=abc123&password=secret&description=test";
		String sanitized = HttpLogSanitizer.sanitizeAndTruncate(body, 500);

		assertTrue(sanitized.contains("token=***"));
		assertTrue(sanitized.contains("password=***"));
		assertTrue(sanitized.contains("description=test"));
		assertFalse(sanitized.contains("abc123"));
		assertFalse(sanitized.contains("secret"));
	}

	@Test
	void sanitizeAndTruncate_truncatesLongBody() {
		String longBody = "x".repeat(30);
		String sanitized = HttpLogSanitizer.sanitizeAndTruncate(longBody, 10);
		assertTrue(sanitized.endsWith(" ..."));
	}
}
