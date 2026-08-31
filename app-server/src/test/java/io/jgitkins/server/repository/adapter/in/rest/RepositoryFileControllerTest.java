package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.port.in.FileLoadUseCase;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RepositoryFileControllerTest {

    @Mock
    private FileLoadUseCase fileLoadUseCase;


    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RepositoryFileController controller =
                new RepositoryFileController(fileLoadUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation
                                .AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void listFiles_usesDefaultRef_whenMissing() throws Exception {
        when(fileLoadUseCase.getAllFiles("team", "repo", "", null)).thenReturn(List.of(
                FileEntry.builder().name("README.md").path("README.md").type("blob").build()
        ));

        mockMvc.perform(get("/repositories/team/repo/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("README.md"));

        verify(fileLoadUseCase).getAllFiles("team", "repo", "", null);
    }

    @Test
    void listFiles_forSpecificRef_returnsFiles() throws Exception {
        when(fileLoadUseCase.getAllFiles("team", "repo", "feature", null))
                .thenReturn(List.of(FileEntry.builder().name("A.java").type("blob").build()));

        mockMvc.perform(get("/repositories/team/repo/files").param("ref", "feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("A.java"));

        verify(fileLoadUseCase).getAllFiles("team", "repo", "feature", null);
    }

    @Test
    void listFileIndex_returnsCompactFields() throws Exception {
        when(fileLoadUseCase.getAllFiles("team", "repo", "main", null)).thenReturn(List.of(
                FileEntry.builder().name("README.md").path("README.md").type("blob").mode("100644").size(12L).build()
        ));

        mockMvc.perform(get("/repositories/team/repo/files/index").param("ref", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("README.md"))
                .andExpect(jsonPath("$.data[0].path").value("README.md"))
                .andExpect(jsonPath("$.data[0].type").value("blob"))
                .andExpect(jsonPath("$.data[0].mode").doesNotExist())
                .andExpect(jsonPath("$.data[0].size").doesNotExist());
    }
}
