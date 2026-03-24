package io.jgitkins.web.application.common;

import java.util.Locale;

public enum UserStatus {
	ACTIVE,
	PENDING,
	UNKNOWN;

	public static UserStatus from(String status) {
		if (status == null || status.isBlank()) {
			return UNKNOWN;
		}
		try {
			return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return UNKNOWN;
		}
	}

	public static boolean isPending(String status) {
		return from(status) == PENDING;
	}
}
