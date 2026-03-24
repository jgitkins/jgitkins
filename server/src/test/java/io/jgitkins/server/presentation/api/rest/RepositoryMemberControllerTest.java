package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import io.jgitkins.server.application.port.in.RepositoryMemberAddUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberQueryUseCase;
import io.jgitkins.server.application.port.in.RepositoryMemberRemoveUseCase;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
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
    private RepositoryMemberAddUseCase repositoryMemberAddUseCase;

    @MockBean
    private RepositoryMemberQueryUseCase repositoryMemberQueryUseCase;

    @MockBean
    private RepositoryMemberRemoveUseCase repositoryMemberRemoveUseCase;

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

        verify(repositoryMemberAddUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.getRepositoryId().equals(1L)
                        && cmd.getUserId().equals(2L)
                        && cmd.getRole() == RepositoryMemberRole.WRITER
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

        verify(repositoryMemberAddUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.getRepositoryId().equals(1L)
                        && cmd.getUserId().equals(3L)
                        && cmd.getRole() == null
        ));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/members/2"))
                .andExpect(status().isNoContent());

        verify(repositoryMemberRemoveUseCase).removeRepositoryMember(1L, 2L);
    }

    @Test
    void listMembers_returnsMemberSummaries() throws Exception {
        when(repositoryMemberQueryUseCase.getRepositoryMembers(1L)).thenReturn(List.of(
                new RepositoryMemberSummary(2L, RepositoryMemberRole.MAINTAINER, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        mockMvc.perform(get("/api/repositories/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2L))
                .andExpect(jsonPath("$.data[0].role").value("MAINTAINER"));

        verify(repositoryMemberQueryUseCase).getRepositoryMembers(1L);
    }
}
