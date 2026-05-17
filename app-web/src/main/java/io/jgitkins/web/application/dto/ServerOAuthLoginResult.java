package io.jgitkins.web.application.dto;

public record ServerOAuthLoginResult(
		String appToken,
		ServerUserProfile user,
		String provider
) {
	public record ServerUserProfile(
			Long id,
			String username,
			String status
	) {
	}
}
