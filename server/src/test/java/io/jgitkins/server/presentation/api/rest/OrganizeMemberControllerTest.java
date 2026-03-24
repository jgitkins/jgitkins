package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizeMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizeMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizeMemberAddUseCase organizeMemberAddUseCase;

    @MockBean
    private OrganizeMemberQueryUseCase organizeMemberQueryUseCase;

    @MockBean
    private OrganizeMemberRemoveUseCase organizeMemberRemoveUseCase;

    @Test
    void addMember_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 2,
                "role", "MEMBER"
        ));

        mockMvc.perform(post("/api/organizes/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(organizeMemberAddUseCase).addOrganizeMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.getOrganizeId().equals(1L)
                        && cmd.getUserId().equals(2L)
                        && cmd.getRole() == OrganizeMemberRole.MEMBER
        ));
    }

    @Test
    void addMember_allowsMissingRoleAndPassesNullRole() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 3
        ));

        mockMvc.perform(post("/api/organizes/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(organizeMemberAddUseCase).addOrganizeMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.getOrganizeId().equals(1L)
                        && cmd.getUserId().equals(3L)
                        && cmd.getRole() == null
        ));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/organizes/1/members/2"))
                .andExpect(status().isNoContent());

        verify(organizeMemberRemoveUseCase).removeOrganizeMember(1L, 2L);
    }

    @Test
    void listMembers_returnsMemberSummaries() throws Exception {
        when(organizeMemberQueryUseCase.getOrganizeMembers(1L)).thenReturn(List.of(
                new OrganizeMemberSummary(2L, OrganizeMemberRole.MEMBER, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        mockMvc.perform(get("/api/organizes/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2L))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"));

        verify(organizeMemberQueryUseCase).getOrganizeMembers(1L);
    }
}
