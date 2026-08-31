package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import java.util.Optional;
import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
import io.jgitkins.server.support.TestAuthentication;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.contract.command.FileUploadInfo;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.repository.application.port.in.FileUploadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.common.presentation.advice.mapper.ApplicationErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.DomainErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.InfrastructureErrorHttpStatusMapper;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class RepositoryContentControllerTest {

        @Mock
        private FileUploadUseCase fileUploadUseCase;

        @Mock
        private FileTreeLoadUseCase fileTreeLoadUseCase;

        @Mock
        private RepositoryLoadUseCase repositoryLoadUseCase;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                CompositeErrorHttpStatusMapper statusMapper = ErrorStatusMappingTestConfig.realMapper();
                RepositoryContentController controller = new RepositoryContentController(
                                fileUploadUseCase,
                                fileTreeLoadUseCase,
                                repositoryLoadUseCase);
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();

                // standaloneSetup has no security filter chain, so @AuthenticationPrincipal is not
                // resolved unless its argument resolver is registered explicitly. Without this the
                // parameter arrives null, every mutation returns 401, and the failure looks like an
                // authorization bug rather than a missing test-harness component.
                this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler(statusMapper))
                                .setCustomArgumentResolvers(
                                                new org.springframework.security.web.method.annotation
                                                                .AuthenticationPrincipalArgumentResolver())
                                .setValidator(validator)
                                .build();
                this.objectMapper = new ObjectMapper();
                TestAuthentication.authenticateAs("7");
        }

        @AfterEach
        void clearSecurityContext() {
                TestAuthentication.clear();
        }



    @Test
    void getTree_returnsWrappedEntries() throws Exception {
        when(fileTreeLoadUseCase.getTree("team", "repo", "main", "src", 7L))
                .thenReturn(List.of(FileEntry.builder().name("README.md").path("README.md").type("blob").build()));

        mockMvc.perform(get("/api/repositories/team/repo/refs/main/tree")
                        .param("dir", "src"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("README.md"))
                .andExpect(jsonPath("$.data[0].type").value("blob"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(fileTreeLoadUseCase).getTree("team", "repo", "main", "src", 7L);
    }

    @Test
    void uploadFileByRepositoryId_resolvesRepositoryKeyAndDelegates() throws Exception {
        // Task 2.64: the controller no longer loads the whole RepositoryResult to derive two strings.
        // It asks the application for the key, so that is what this test stubs.
        when(repositoryLoadUseCase.resolveRepositoryKey(10L))
                .thenReturn(Optional.of(new RepositoryKey("users/alice", "sample-repo")));

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/repositories/{repositoryId}/files", 10L)
                        .file(filePart)
                        .param("branch", "main")
                        .param("path", "docs/hello.txt")
                        .param("message", "add file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("File uploaded and committed."))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(fileUploadUseCase).uploadFileToRepository(eq(7L),
                eq("users/alice"),
                eq("sample-repo"),
                eq("main"),
                any(),
                any(FileUploadInfo.class)
        );
    }

    @Test
    void uploadFileByRepositoryId_returnsNotFound_whenRepositoryPathInvalid() throws Exception {
        // An unusable path is empty from the boundary rather than a result the controller must inspect;
        // the 404 stays the controller's decision, which is what preserves the existing status.
        when(repositoryLoadUseCase.resolveRepositoryKey(11L)).thenReturn(Optional.empty());

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/repositories/{repositoryId}/files", 11L)
                        .file(filePart)
                        .param("branch", "main")
                        .param("path", "docs/hello.txt")
                        .param("message", "add file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPO-404"));
    }

        @Test
        void uploadFile_withRequestPart_delegatesToUseCase() throws Exception {
                MockMultipartFile filePart = new MockMultipartFile(
                                "file",
                                "hello.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "hello".getBytes());
                FileUploadInfo info = FileUploadInfo.builder().filePath("docs/hello.txt")
                                .commitMessage("add").authorName("alice").authorEmail("alice@test.com").build();
                MockMultipartFile requestPart = new MockMultipartFile(
                                "request",
                                "",
                                MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(info));

                mockMvc.perform(multipart("/api/repositories/{taskCd}/{repoName}/files/{branch}", "alice",
                                "sample-repo", "main")
                                .file(filePart)
                                .file(requestPart)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").value("File uploaded and committed."));

                verify(fileUploadUseCase).uploadFileToRepository(eq(7L),
                                eq("alice"),
                                eq("sample-repo"),
                                eq("main"),
                                any(),
                                any(FileUploadInfo.class));
        }

        /** The malformed subjects the identity resolver must refuse. Zero is malformed, not absent. */

        private MockMultipartFile textPart() {
                return new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());
        }

        private org.springframework.mock.web.MockMultipartFile infoPart() throws Exception {
                FileUploadInfo info = FileUploadInfo.builder()
                                .filePath("docs/hello.txt").commitMessage("add file").build();
                return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                                objectMapper.writeValueAsBytes(info));
        }

        @Test
        void uploadFile_rejectsAnUnauthenticatedCallerWithAuth001AndNoCommit() throws Exception {
                TestAuthentication.clear();
                        mockMvc.perform(multipart("/api/repositories/{namespace}/{repoName}/files/{branch}",
                                        "alice", "sample-repo", "main")
                                        .file(textPart()).file(infoPart())
                                        .contentType(MediaType.MULTIPART_FORM_DATA))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.error.code").value("AUTH-001"));
                // The upload never reaches the use case, so a denied request has not read the multipart
                // into commit files or spent temp space on content it will never commit.
                verifyNoInteractions(fileUploadUseCase);
        }

        @Test
        void uploadFile_rejectsAnonymousWithAuth001AndNoCommit() throws Exception {
                TestAuthentication.clear();
                mockMvc.perform(multipart("/api/repositories/{namespace}/{repoName}/files/{branch}",
                                "alice", "sample-repo", "main")
                                .file(textPart()).file(infoPart())
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
                verifyNoInteractions(fileUploadUseCase);
        }

        @Test
        void uploadFile_rejectsNonMemberWithoutCommit() throws Exception {
                doThrow(new ApplicationException(ApplicationErrorCode.ACCESS_DENIED,
                                "Insufficient permission to commit to repository: alice/sample-repo"))
                                .when(fileUploadUseCase).uploadFileToRepository(
                                                eq(7L), eq("alice"), eq("sample-repo"), eq("main"), any(),
                                                any(FileUploadInfo.class));

                mockMvc.perform(multipart("/api/repositories/{namespace}/{repoName}/files/{branch}",
                                "alice", "sample-repo", "main")
                                .file(textPart()).file(infoPart())
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isForbidden());
        }

        @Test
        void uploadFile_returnsNotFound() throws Exception {
                doThrow(new RepositoryNotFoundException("alice", "sample-repo"))
                                .when(fileUploadUseCase).uploadFileToRepository(
                                                eq(7L), eq("alice"), eq("sample-repo"), eq("main"), any(),
                                                any(FileUploadInfo.class));

                mockMvc.perform(multipart("/api/repositories/{namespace}/{repoName}/files/{branch}",
                                "alice", "sample-repo", "main")
                                .file(textPart()).file(infoPart())
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isNotFound());
        }

        @Test
        void uploadFileByRepositoryId_rejectsAnUnauthenticatedCallerWithAuth001AndNoCommit() throws Exception {
                TestAuthentication.clear();
                        mockMvc.perform(multipart("/api/repositories/{repositoryId}/files", 10L)
                                        .file(textPart())
                                        .param("branch", "main").param("path", "docs/hello.txt")
                                        .param("message", "add file"))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.error.code").value("AUTH-001"));
                verifyNoInteractions(fileUploadUseCase);
                // And the repository was never looked up, so this route cannot be used to probe whether a
                // repository id exists without a valid credential.
                verifyNoInteractions(repositoryLoadUseCase);
        }

        @Test
        void uploadFileByRepositoryId_rejectsAnonymousWithAuth001AndNoCommit() throws Exception {
                TestAuthentication.clear();
                mockMvc.perform(multipart("/api/repositories/{repositoryId}/files", 10L)
                                .file(textPart())
                                .param("branch", "main").param("path", "docs/hello.txt")
                                .param("message", "add file"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
                verifyNoInteractions(fileUploadUseCase);
                verifyNoInteractions(repositoryLoadUseCase);
        }

        @Test
        void uploadFileByRepositoryId_rejectsNonMemberWithoutCommit() throws Exception {
                when(repositoryLoadUseCase.resolveRepositoryKey(10L))
                                .thenReturn(Optional.of(new RepositoryKey("users/alice", "sample-repo")));
                doThrow(new ApplicationException(ApplicationErrorCode.ACCESS_DENIED,
                                "Insufficient permission to commit to repository: users/alice/sample-repo"))
                                .when(fileUploadUseCase).uploadFileToRepository(
                                                eq(7L), eq("users/alice"), eq("sample-repo"), eq("main"), any(),
                                                any(FileUploadInfo.class));

                mockMvc.perform(multipart("/api/repositories/{repositoryId}/files", 10L)
                                .file(textPart())
                                .param("branch", "main").param("path", "docs/hello.txt")
                                .param("message", "add file"))
                                .andExpect(status().isForbidden());
        }
}
