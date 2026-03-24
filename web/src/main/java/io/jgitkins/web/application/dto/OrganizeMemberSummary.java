package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record OrganizeMemberSummary(
		Long userId,
		String role,
		LocalDateTime joinedAt
) {
}
