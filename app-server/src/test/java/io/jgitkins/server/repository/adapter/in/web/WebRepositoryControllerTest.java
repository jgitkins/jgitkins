package io.jgitkins.server.repository.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.support.TestAuthentication;
import java.util.List;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The web-facing read routes carry the same requester the REST ones do.
 *
 * <p>Worth testing separately rather than assuming symmetry: these two routes are the ones a browser hits
 * while logged out, so they are where an over-strict change would be noticed by users rather than by a
 * test. Anonymous must stay anonymous — a null requester reaching the use case, not a 401.
 */
@WebMvcTest(WebRepositoryController.class)
@Import({RequesterUserIdResolver.class, ErrorStatusMappingTestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class WebRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryLoadUseCase repositoryLoadUseCase;

    @MockBean
    private RepositoryOverviewUseCase repositoryOverviewUseCase;

    @BeforeEach
    void authenticateRequester() {
        TestAuthentication.authenticateAs("7");
    }

    @AfterEach
    void clearAuthentication() {
        TestAuthentication.clear();
    }

    @Test
    void getUserRepositories_preservesPublicAnonymousRead() throws Exception {
        TestAuthentication.clear();
        when(repositoryLoadUseCase.loadUserRepositories(null, "alice")).thenReturn(List.of(publicRepository()));

        mockMvc.perform(get("/api/internal/repositories/users/alice")).andExpect(status().isOk());

        // Null, not a rejection. This route is what a logged-out visitor hits to browse someone's public
        // repositories; the visibility filter narrows the result, the adapter does not refuse the request.
        verify(repositoryLoadUseCase).loadUserRepositories(null, "alice");
    }

    @Test
    void getRepositoryOverviewByPath_passesExplicitRequester() throws Exception {
        when(repositoryOverviewUseCase.getOverviewByPath(7L, "org", "repo", "main")).thenReturn(
                new RepositoryOverviewResult(publicRepository(), List.of(), List.of(), "main", "OWNER", true));

        mockMvc.perform(get("/api/internal/repositories/org/repo/overview").param("branch", "main"))
                .andExpect(status().isOk());

        verify(repositoryOverviewUseCase).getOverviewByPath(7L, "org", "repo", "main");
    }

    @Test
    void getRepositoryOverviewByPath_stillWorksAnonymously() throws Exception {
        TestAuthentication.clear();
        when(repositoryOverviewUseCase.getOverviewByPath(null, "org", "repo", null)).thenReturn(
                new RepositoryOverviewResult(publicRepository(), List.of(), List.of(), "main",
                        "PUBLIC_READ_ONLY", false));

        mockMvc.perform(get("/api/internal/repositories/org/repo/overview")).andExpect(status().isOk());

        verify(repositoryOverviewUseCase).getOverviewByPath(null, "org", "repo", null);
    }

    private static RepositoryResult publicRepository() {
        return new RepositoryResult(1L, "USER", "repo", "org/repo", "main", "PUBLIC",
                null, 7L, null, "/org/repo.git", null, false, null, null, null);
    }
}
