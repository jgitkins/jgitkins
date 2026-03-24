package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.application.dto.FileUploadInfo;
import io.jgitkins.server.application.factory.CommitFileFactory;
import io.jgitkins.server.application.port.out.CommitGitPort;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryFileServiceTest {

    @Mock
    private CommitFileFactory commitFileFactory;

    @Mock
    private CommitGitPort commitGitPort;

    @Mock
    private FileGitPort fileGitPort;

    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;

    @InjectMocks
    private RepositoryFileService service;

    @Test
    void uploadFileToRepository_commitsPreparedFiles() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        FileUploadInfo request = new FileUploadInfo();
        request.setCommitMessage("msg");
        request.setAuthorName("author");
        request.setAuthorEmail("a@b.com");
        List<CommitFile> files = List.of(CommitFile.builder().path("README.md").build());
        when(commitFileFactory.prepareUploadFile(file, request)).thenReturn(files);

        service.uploadFileToRepository("task", "repo", "main", file, request);

        verify(repositoryAccessValidator).validateCanCommit("task", "repo");
        verify(commitGitPort).commit(eq("task"), eq("repo"), eq("main"),
                eq("msg"), eq("author"), eq("a@b.com"), eq(files));
    }

    @Test
    void uploadFileToRepository_usesDefaultAuthorWhenMissing() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        FileUploadInfo request = new FileUploadInfo();
        request.setCommitMessage("msg");
        List<CommitFile> files = List.of(CommitFile.builder().path("README.md").build());
        when(commitFileFactory.prepareUploadFile(file, request)).thenReturn(files);

        service.uploadFileToRepository("task", "repo", "main", file, request);

        verify(repositoryAccessValidator).validateCanCommit("task", "repo");
        verify(commitGitPort).commit(eq("task"), eq("repo"), eq("main"),
                eq("msg"), eq("jgitkins"), eq("no-reply@jgitkins.local"), eq(files));
    }

    @Test
    void getTree_delegatesToFileGitPort() {
        when(fileGitPort.listTree("ns", "repo", "main", "src")).thenReturn(Collections.emptyList());

        service.getTree("ns", "repo", "main", "src");

        verify(fileGitPort).listTree("ns", "repo", "main", "src");
    }

    @Test
    void getAllFiles_delegatesToFileGitPort() {
        when(fileGitPort.listAllFiles("ns", "repo", "main")).thenReturn(Collections.emptyList());

        service.getAllFiles("ns", "repo", "main");

        verify(fileGitPort).listAllFiles("ns", "repo", "main");
    }
}
