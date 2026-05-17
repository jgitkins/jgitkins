package io.jgitkins.server.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserUsernameUpdateRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username allows only letters, numbers, dot, hyphen, or underscore.")
        String username
) {
}
