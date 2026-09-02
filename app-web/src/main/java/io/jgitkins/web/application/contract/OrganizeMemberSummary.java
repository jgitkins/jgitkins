package io.jgitkins.web.application.contract;

import java.time.LocalDateTime;

public record OrganizeMemberSummary(
		Long userId,
		String role,
		LocalDateTime joinedAt
) {
}
