package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadInfo {

    @NotBlank(message = "File path is required")
    private String filePath;

    @NotBlank(message = "Commit message is required")
    private String commitMessage;

    private String authorName;

    @Email(message = "Invalid author email format")
    private String authorEmail;

    // Added for convenience in builders if needed
    private String targetPath;
    private Object file; // MultipartFile 등
}
