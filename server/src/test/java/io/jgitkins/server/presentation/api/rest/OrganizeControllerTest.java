package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.presentation.dto.OrganizeCreationRequest;
import io.jgitkins.server.presentation.mapper.OrganizeRequestMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizeController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizeCreationUseCase organizeCreationUseCase;

    @MockBean
    private OrganizeLoadUseCase organizeLoadUseCase;

    @MockBean
    private OrganizeDeletionUseCase organizeDeletionUseCase;

    @MockBean
    private OrganizeRequestMapper organizeRequestMapper;

    @Test
    void createOrganize_returnsCreatedResponse() throws Exception {
        OrganizeCreationCommand command = new OrganizeCreationCommand("core-team", 1L, "Core Team");
        OrganizeCreationResult result = new OrganizeCreationResult(10L, "core-team", "Core Team", 1L, null, null);

        when(organizeRequestMapper.toCommand(org.mockito.ArgumentMatchers.any(OrganizeCreationRequest.class)))
                .thenReturn(command);
        when(organizeCreationUseCase.createOrganize(command)).thenReturn(result);

        mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "core-team",
                                "ownerId", 1,
                                "description", "Core Team"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/organizes/10")))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.name").value("core-team"));

        verify(organizeCreationUseCase).createOrganize(command);
    }

    @Test
    void getOrganizes_returnsList() throws Exception {
        when(organizeLoadUseCase.getOrganizes()).thenReturn(List.of(
                new OrganizeCreationResult(1L, "org-a", null, null, null, null),
                new OrganizeCreationResult(2L, "org-b", null, null, null, null)
        ));

        mockMvc.perform(get("/api/organizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("org-a"))
                .andExpect(jsonPath("$.data[1].name").value("org-b"));

        verify(organizeLoadUseCase).getOrganizes();
    }

    @Test
    void getAccessibleOrganizes_returnsList() throws Exception {
        when(organizeLoadUseCase.getAccessibleOrganizes()).thenReturn(List.of(
                new OrganizeCreationResult(3L, "org-c", null, null, null, null)
        ));

        mockMvc.perform(get("/api/organizes/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L));

        verify(organizeLoadUseCase).getAccessibleOrganizes();
    }

    @Test
    void getOrganize_returnsSingleResult() throws Exception {
        when(organizeLoadUseCase.getOrganize(7L)).thenReturn(
                new OrganizeCreationResult(7L, "org-seven", null, null, null, null)
        );

        mockMvc.perform(get("/api/organizes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7L))
                .andExpect(jsonPath("$.data.name").value("org-seven"));

        verify(organizeLoadUseCase).getOrganize(7L);
    }

    @Test
    void deleteOrganize_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/organizes/9"))
                .andExpect(status().isNoContent());

        verify(organizeDeletionUseCase).deleteOrganize(9L);
    }
}
