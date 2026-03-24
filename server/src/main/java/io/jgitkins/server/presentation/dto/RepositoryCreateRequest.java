package io.jgitkins.server.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RepositoryCreateRequest(
        @Schema(description = "Repository name (unique within owner namespace).")
        @NotBlank(message = "repoName is required")
        @Size(max = 255, message = "repoName must be 255 characters or fewer")
        String repoName,

        @Schema(description = "Default branch name. Optional.")
        @Size(max = 255, message = "mainBranch must be 255 characters or fewer")
        String mainBranch,

        @Schema(description = "Initial commit author name. Optional.")
        @Size(max = 255, message = "username must be 255 characters or fewer")
        String username,

        @Schema(description = "Initial commit author email. Optional.")
        @Email(message = "email must be a valid email address")
        @Size(max = 255, message = "email must be 255 characters or fewer")
        String email,

        @Schema(description = "Create README.md on initial commit.")
        boolean readme,

        @Schema(description = "Initial commit message. Optional.")
        @Size(max = 500, message = "message must be 500 characters or fewer")
        String message,

        @Schema(description = "Owner type for the repository.")
        @NotBlank(message = "ownerType is required")
        @Size(max = 50, message = "ownerType must be 50 characters or fewer")
        String ownerType,

        @Schema(description = "Organization id when ownerType is ORGANIZATION.")
        @Positive(message = "organizeId must be positive")
        Long organizeId,

        @Schema(description = "Repository visibility. Optional.")
        @Size(max = 50, message = "visibility must be 50 characters or fewer")
        String visibility,

        @Schema(description = "Repository description. Optional.")
        @Size(max = 1000, message = "description must be 1000 characters or fewer")
        String description,

        @Schema(description = "Credential id for remote access. Optional.")
        @Size(max = 255, message = "credentialId must be 255 characters or fewer")
        String credentialId
) {

    @AssertTrue(message = "ownerType requires organizeId only when ownerType is ORGANIZATION.")
    public boolean isOwnerSelectionValid() {
        if (ownerType == null || ownerType.isBlank()) {
            return false;
        }
        String normalized = ownerType.trim().toUpperCase();
        if ("USER".equals(normalized)) {
            return organizeId == null;
        }
        if ("ORGANIZATION".equals(normalized)) {
            return organizeId != null;
        }
        return false;
    }
}
