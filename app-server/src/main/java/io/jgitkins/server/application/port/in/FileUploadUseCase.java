package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.FileUploadInfo;
import org.springframework.web.multipart.MultipartFile;


public interface FileUploadUseCase {
    void uploadFileToRepository(String namespace, String repoName, String branch, MultipartFile file, FileUploadInfo request);
}
