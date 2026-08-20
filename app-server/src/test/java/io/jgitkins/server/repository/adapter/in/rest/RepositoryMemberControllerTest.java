package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryMemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class RepositoryMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositoryMemberManagementUseCase repositoryMemberManagementUseCase;

    @MockBean
    private RepositoryMemberLoadUseCase repositoryMemberLoadUseCase;

    @Test
    void addMember_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 2,
                "role", "WRITER"
        ));

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(repositoryMemberManagementUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.repositoryId().equals(1L)
                        && cmd.userId().equals(2L)
                        && cmd.role() == RepositoryMemberRole.WRITER
        ));
    }

    @Test
    void addMember_allowsMissingRoleAndPassesNullRole() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 3
        ));

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(repositoryMemberManagementUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.repositoryId().equals(1L)
                        && cmd.userId().equals(3L)
                        && cmd.role() == null
        ));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/members/2"))
                .andExpect(status().isNoContent());

        verify(repositoryMemberManagementUseCase).removeRepositoryMember(1L, 2L);
    }

    @Test
    void listMembers_returnsMemberSummaries() throws Exception {
        when(repositoryMemberLoadUseCase.getRepositoryMembers(1L)).thenReturn(List.of(
                new RepositoryMemberSummary(2L, RepositoryMemberRole.MAINTAINER, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        mockMvc.perform(get("/api/repositories/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2L))
                .andExpect(jsonPath("$.data[0].role").value("MAINTAINER"));

        verify(repositoryMemberLoadUseCase).getRepositoryMembers(1L);
    }
}
