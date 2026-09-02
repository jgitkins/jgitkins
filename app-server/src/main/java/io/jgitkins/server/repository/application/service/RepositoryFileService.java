package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.internal.CommitFile;
import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import io.jgitkins.server.repository.application.port.in.FileLoadUseCase;
import io.jgitkins.server.repository.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.repository.application.port.in.FileUploadUseCase;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.repository.application.support.CommitFilePreparer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RepositoryFileService implements FileUploadUseCase,
        FileLoadUseCase,
        FileTreeLoadUseCase {

    private static final String DEFAULT_AUTHOR_NAME = "jgitkins";
    private static final String DEFAULT_AUTHOR_EMAIL = "no-reply@jgitkins.local";

    private final CommitFilePreparer commitFilePreparer;
    private final CommitGitPort commitGitPort;
    private final FileGitPort fileGitPort;
    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    @Transactional
    public void uploadFileToRepository(Long requesterUserId,
            String namespace,
            String repoName,
            String branch,
            MultipartFile file,
            FileUploadInfo request) {
        // Before the multipart is read into commit files: a denied upload must not have spent memory
        // or temp space on content it will never commit.
        repositoryAccessValidator.validateCanCommit(namespace, repoName, requesterUserId);

        List<CommitFile> files = commitFilePreparer.prepareUploadFile(file, request);

        commitGitPort.commit(namespace,
                repoName,
                branch,
                request.getCommitMessage(),
                resolveAuthorName(request),
                resolveAuthorEmail(request),
                files);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntry> getTree(String namespace,
            String repoName,
            String branch,
            String directory,
            Long requesterUserId) {
        repositoryAccessValidator.validateReadAccess(namespace, repoName, requesterUserId);
        return fileGitPort.listTree(namespace, repoName, branch, directory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntry> getAllFiles(String namespace, String repoName, String reference,
            Long requesterUserId) {
        repositoryAccessValidator.validateReadAccess(namespace, repoName, requesterUserId);
        return fileGitPort.listAllFiles(namespace, repoName, reference);
    }

    private String resolveAuthorName(FileUploadInfo request) {
        // TODO: request 내 AuthorName 값 존재 여부는 API 요청 단계(@Valid)에서 검증 필요
        return request.getAuthorName() != null ? request.getAuthorName() : DEFAULT_AUTHOR_NAME;
    }

    private String resolveAuthorEmail(FileUploadInfo request) {
        // TODO: request 내 AuthorEmail 값 존재 여부는 API 요청 단계(@Valid)에서 검증 필요
        return request.getAuthorEmail() != null ? request.getAuthorEmail() : DEFAULT_AUTHOR_EMAIL;
    }
}
