package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.server.application.factory.CommitFileFactory;
import io.jgitkins.server.application.port.in.FileLoadUseCase;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.FileUploadUseCase;
import io.jgitkins.server.application.port.out.CommitGitPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
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

    private final CommitFileFactory commitFileFactory;
    private final CommitGitPort commitGitPort;
    private final FileGitPort fileGitPort;
    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    @Transactional
    public void uploadFileToRepository(String namespace,
            String repoName,
            String branch,
            MultipartFile file,
            FileUploadInfo request) {
        repositoryAccessValidator.validateCanCommit(namespace, repoName);

        List<CommitFile> files = commitFileFactory.prepareUploadFile(file, request);

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
            String directory) {
        return fileGitPort.listTree(namespace, repoName, branch, directory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileEntry> getAllFiles(String namespace, String repoName, String reference) {
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
