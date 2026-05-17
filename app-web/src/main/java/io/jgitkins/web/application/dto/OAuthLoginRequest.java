package io.jgitkins.web.application.dto;

public record OAuthLoginRequest(
		String provider,
		String subject,
		String email,
		String name,
		boolean emailVerified,
		String avatarUrl
) {
}
