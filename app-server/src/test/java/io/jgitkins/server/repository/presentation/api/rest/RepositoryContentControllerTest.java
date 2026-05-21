package io.jgitkins.server.repository.presentation.api.rest;

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
import java.util.List;
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
                CompositeErrorHttpStatusMapper statusMapper = new CompositeErrorHttpStatusMapper(
                                List.of(
                                                new DomainErrorHttpStatusMapper(),
                                                new ApplicationErrorHttpStatusMapper(),
                                                new InfrastructureErrorHttpStatusMapper()));
                RepositoryContentController controller = new RepositoryContentController(
                                fileUploadUseCase,
                                fileTreeLoadUseCase,
                                repositoryLoadUseCase);
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();

                this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler(statusMapper))
                                .setValidator(validator)
                                .build();
                this.objectMapper = new ObjectMapper();
        }

    @Test
    void getTree_returnsWrappedEntries() throws Exception {
        when(fileTreeLoadUseCase.getTree("team", "repo", "main", "src"))
                .thenReturn(List.of(FileEntry.builder().name("README.md").path("README.md").type("blob").build()));

        mockMvc.perform(get("/api/repositories/team/repo/refs/main/tree")
                        .param("dir", "src"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("README.md"))
                .andExpect(jsonPath("$.data[0].type").value("blob"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(fileTreeLoadUseCase).getTree("team", "repo", "main", "src");
    }

    @Test
    void uploadFileByRepositoryId_resolvesRepositoryKeyAndDelegates() throws Exception {
        when(repositoryLoadUseCase.loadRepository(10L)).thenReturn(
                new RepositoryResult(10L, null, null, null, null, null, null, null, null, "users/alice/sample-repo.git", null, false, null, null, null)
        );

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

        verify(fileUploadUseCase).uploadFileToRepository(
                eq("users/alice"),
                eq("sample-repo"),
                eq("main"),
                any(),
                any(FileUploadInfo.class)
        );
    }

    @Test
    void uploadFileByRepositoryId_returnsNotFound_whenRepositoryPathInvalid() throws Exception {
        when(repositoryLoadUseCase.loadRepository(11L)).thenReturn(
                new RepositoryResult(11L, null, null, "also-invalid", null, null, null, null, null, "invalid-path-only", null, false, null, null, null)
        );

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

                verify(fileUploadUseCase).uploadFileToRepository(
                                eq("alice"),
                                eq("sample-repo"),
                                eq("main"),
                                any(),
                                any(FileUploadInfo.class));
        }
}
