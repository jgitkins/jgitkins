package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.FileUploadInfo;
import org.springframework.web.multipart.MultipartFile;


public interface FileUploadUseCase {
    /**
     * @param requesterUserId the authenticated caller, resolved once by the inbound adapter. First
     *     parameter by convention across every mutation in this context, so a caller cannot pass the
     *     repository id where the actor belongs and have it compile.
     */
    void uploadFileToRepository(Long requesterUserId, String namespace, String repoName, String branch,
                               MultipartFile file, FileUploadInfo request);
}
