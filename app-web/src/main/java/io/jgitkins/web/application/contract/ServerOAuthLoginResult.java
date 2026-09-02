package io.jgitkins.web.application.contract;

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
