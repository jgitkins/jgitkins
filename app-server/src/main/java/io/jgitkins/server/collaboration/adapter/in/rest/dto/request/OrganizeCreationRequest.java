package io.jgitkins.server.collaboration.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The constraints mirror {@code OrganizeName}, which rejects null, blank after trim, and anything
 * outside {@code [A-Za-z0-9_-]}. Expressed here so a violating name is refused at the boundary with a
 * 400 instead of reaching the value object, which throws IllegalArgumentException and answers 500.
 *
 * <p>{@code description} carries no constraint because no domain rule reads it.
 */
public record OrganizeCreationRequest(
        @NotBlank(message = "Organize name must not be blank")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$",
                message = "Organize name allows only alphanumeric characters, hyphen, or underscore")
        String name,
        String description
) {
}
