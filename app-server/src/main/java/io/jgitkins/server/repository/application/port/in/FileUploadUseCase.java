package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import org.springframework.web.multipart.MultipartFile;


public interface FileUploadUseCase {
    void uploadFileToRepository(String namespace, String repoName, String branch, MultipartFile file, FileUploadInfo request);
}
