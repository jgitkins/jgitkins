package io.jgitkins.server.collaboration.adapter.in.rest.contract.request;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * {@code userId} mirrors {@code MemberUserId}, which rejects null and any value at or below zero.
 *
 * <p>{@code role} is deliberately unconstrained: a missing role is valid and is passed through as null,
 * which {@code addMember_allowsMissingRoleAndPassesNullRole} pins.
 */
public record OrganizeMemberAddRequest(
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be a positive value")
        Long userId,
        OrganizeMemberRole role
) {
}
