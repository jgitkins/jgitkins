package io.jgitkins.server.common.factory;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import io.jgitkins.server.common.infrastructure.exception.FileReadFailedException;
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
            // TODO: File 을 다루는건 기술적인 영역이다보니, FileReadFailedException 이 되어버렸는데, 애플리케이션 계층에서 Infrastructure 를 의존하는구조가 되어버림.. 그래도 되는지 검토 필요
            throw new FileReadFailedException("Failed to read upload file content", e);
        }
    }

    private String stripGitSuffix(String name) {
        return name != null && name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}
