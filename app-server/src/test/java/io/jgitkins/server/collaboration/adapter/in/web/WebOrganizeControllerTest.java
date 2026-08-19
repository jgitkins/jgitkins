package io.jgitkins.server.collaboration.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WebOrganizeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrganizeLoadUseCase organizeLoadUseCase;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new WebOrganizeController(organizeLoadUseCase)
        ).build();
    }

    @Test
    void getAccessibleOrganizes_preservesApplicationResultPayload() throws Exception {
        when(organizeLoadUseCase.getAccessibleOrganizes()).thenReturn(List.of(
                new OrganizeCreationResult(3L, "org-c", "description", 7L, null, null)
        ));

        mockMvc.perform(get("/api/internal/organizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L))
                .andExpect(jsonPath("$.data[0].name").value("org-c"))
                .andExpect(jsonPath("$.data[0].description").value("description"))
                .andExpect(jsonPath("$.data[0].ownerId").value(7L))
                .andExpect(jsonPath("$.data[0].createdAt").value((Object) null))
                .andExpect(jsonPath("$.data[0].updatedAt").value((Object) null));
    }
}
