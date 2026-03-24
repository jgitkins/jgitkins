package io.jgitkins.server.application.factory;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.server.application.exception.ApplicationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CommitFileFactory {

    public List<CommitFile> prepareInitialFile(String repoName) {
        String displayName = stripGitSuffix(repoName);
        String readmeContent = "# " + displayName + "\n";

        return List.of(CommitFile.builder()
                .path("README.md")
                .content(readmeContent.getBytes(StandardCharsets.UTF_8))
                .build());
    }

    public List<CommitFile> prepareUploadFile(MultipartFile file, FileUploadInfo request) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String targetPath = StringUtils.hasText(request.getFilePath()) ? request.getFilePath() : file.getOriginalFilename();
        if (!StringUtils.hasText(targetPath)) {
            throw new IllegalArgumentException("File path is missing");
        }

        try {
            return List.of(CommitFile.builder()
                    .path(targetPath)
                    .content(file.getBytes())
                    .build());
        } catch (IOException e) {
            throw new ApplicationException(ApplicationErrorCode.FILE_READ_FAILED, "Failed to read upload file content", e);
        }
    }

    private String stripGitSuffix(String name) {
        return name != null && name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}
