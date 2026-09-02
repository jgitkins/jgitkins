package io.jgitkins.server.identity.access.adapter.in.rest.contract.request;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code AdminUserService:75-81} normalizes a null status to the empty string and hands it to
 * {@code UserStatus.fromString}, which rejects it. The constraint moves that rejection to the boundary
 * so the caller gets a 400 naming the field instead of whatever the enum parse produces.
 */
public record UserStatusUpdateRequest(
        @NotBlank(message = "status must not be blank")
        String status
) {
}
