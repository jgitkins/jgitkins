package io.jgitkins.server.repository.adapter.in.rest.contract.request;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * {@code userId} mirrors {@code RepositoryMemberUserId}, which rejects null and any value at or below
 * zero.
 *
 * <p>{@code role} is deliberately unconstrained: a missing role is valid and is passed through as null,
 * which {@code addMember_allowsMissingRoleAndPassesNullRole} pins.
 */
public record RepositoryMemberAddRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be a positive value")
        Long userId,
        RepositoryMemberRole role
) {
}
