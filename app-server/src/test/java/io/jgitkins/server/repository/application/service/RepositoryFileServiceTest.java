package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.repository.application.support.CommitFilePreparer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryFileServiceTest {

    @Mock
    private CommitFilePreparer commitFilePreparer;

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
        when(commitFilePreparer.prepareUploadFile(file, request)).thenReturn(files);

        service.uploadFileToRepository(7L, "task", "repo", "main", file, request);

        verify(repositoryAccessValidator).validateCanCommit("task", "repo", 7L);
        verify(commitGitPort).commit(eq("task"), eq("repo"), eq("main"),
                eq("msg"), eq("author"), eq("a@b.com"), eq(files));
    }

    @Test
    void uploadFileToRepository_usesDefaultAuthorWhenMissing() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        FileUploadInfo request = new FileUploadInfo();
        request.setCommitMessage("msg");
        List<CommitFile> files = List.of(CommitFile.builder().path("README.md").build());
        when(commitFilePreparer.prepareUploadFile(file, request)).thenReturn(files);

        service.uploadFileToRepository(7L, "task", "repo", "main", file, request);

        verify(repositoryAccessValidator).validateCanCommit("task", "repo", 7L);
        verify(commitGitPort).commit(eq("task"), eq("repo"), eq("main"),
                eq("msg"), eq("jgitkins"), eq("no-reply@jgitkins.local"), eq(files));
    }

    // --- task 2.125: both read paths went straight to git ---------------------------------------
    //
    // Neither method checked visibility. RepositoryFileController exposes both without a principal
    // and SecurityConfig is permitAll, so an anonymous caller could list every file name and path in
    // a private repository. The task named the app-web route; the server answered the same thing
    // directly, which is why fixing app-web alone would have closed nothing.

    private static final long REQUESTER = 7L;

    @Test
    void getTree_readsOnlyAfterTheVisibilityCheckPasses() {
        when(fileGitPort.listTree("ns", "repo", "main", "src")).thenReturn(Collections.emptyList());

        service.getTree("ns", "repo", "main", "src", REQUESTER);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(repositoryAccessValidator, fileGitPort);
        order.verify(repositoryAccessValidator).validateReadAccess("ns", "repo", REQUESTER);
        order.verify(fileGitPort).listTree("ns", "repo", "main", "src");
    }

    @Test
    void getTree_doesNotTouchGitWhenTheRepositoryIsNotVisible() {
        org.mockito.Mockito.doThrow(new RepositoryNotFoundException())
                .when(repositoryAccessValidator).validateReadAccess("ns", "repo", REQUESTER);

        assertThrows(RepositoryNotFoundException.class,
                () -> service.getTree("ns", "repo", "main", "src", REQUESTER));
        verify(fileGitPort, org.mockito.Mockito.never()).listTree(any(), any(), any(), any());
    }

    @Test
    void getAllFiles_readsOnlyAfterTheVisibilityCheckPasses() {
        when(fileGitPort.listAllFiles("ns", "repo", "main")).thenReturn(Collections.emptyList());

        service.getAllFiles("ns", "repo", "main", REQUESTER);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(repositoryAccessValidator, fileGitPort);
        order.verify(repositoryAccessValidator).validateReadAccess("ns", "repo", REQUESTER);
        order.verify(fileGitPort).listAllFiles("ns", "repo", "main");
    }

    @Test
    void getAllFiles_doesNotTouchGitWhenTheRepositoryIsNotVisible() {
        org.mockito.Mockito.doThrow(new RepositoryNotFoundException())
                .when(repositoryAccessValidator).validateReadAccess("ns", "repo", null);

        // Anonymous is passed through as null rather than rejected up front: a public repository is
        // readable without credentials, and canRead is what decides.
        assertThrows(RepositoryNotFoundException.class,
                () -> service.getAllFiles("ns", "repo", "main", null));
        verify(fileGitPort, org.mockito.Mockito.never()).listAllFiles(any(), any(), any());
    }
}
