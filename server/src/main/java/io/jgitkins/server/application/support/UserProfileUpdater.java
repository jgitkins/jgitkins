package io.jgitkins.server.application.support;

import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserIdentity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class UserProfileUpdater {

	public User applyUserUpdates(User user,
			String email,
			String name,
			String avatarUrl,
			LocalDateTime loginAt) {
		User updated = maybeUpdateUser(user, email, name, avatarUrl);
		return updated.touchLogin(loginAt);
	}

	public UserIdentity updateIdentityIfChanged(UserIdentity identity,
			String email,
			boolean emailVerified,
			String name,
			String avatarUrl) {
		// TODO: 문자열 기본 검증 책임은 호출자(Controller 또는 상위 Service)로 이전 (API/DTO 단 @Valid 활용)
		boolean changed = !equals(identity.getEmail(), email)
				|| identity.isEmailVerified() != emailVerified
				|| !equals(identity.getName(), name)
				|| !equals(identity.getAvatarUrl(), avatarUrl);
		return changed ? identity.updateProfile(email, emailVerified, name, avatarUrl) : identity;
	}

	private User maybeUpdateUser(User user, String email, String name, String avatarUrl) {
		// TODO: 문자열 기본 검증 책임은 호출자(Controller 또는 상위 Service)로 이전 (API/DTO 단 @Valid 활용)
		boolean changed = !equals(user.getEmail(), email)
				|| !equals(user.getDisplayName(), name)
				|| !equals(user.getAvatarUrl(), avatarUrl);
		return changed ? user.updateProfile(email, name, avatarUrl) : user;
	}

	private boolean equals(String left, String right) {
		if (left == null) {
			return right == null;
		}
		return left.equals(right);
	}

}
