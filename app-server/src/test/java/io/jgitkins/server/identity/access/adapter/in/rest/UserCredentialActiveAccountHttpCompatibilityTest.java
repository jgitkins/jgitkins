package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.common.presentation.advice.mapper.ApplicationErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.DomainErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.InfrastructureErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.PresentationErrorHttpStatusMapper;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialIssueResult;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.identity.access.application.port.in.UserCredentialIssueUseCase;
import io.jgitkins.server.identity.access.application.port.in.UserCredentialQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.UserCredentialRevokeUseCase;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserCredentialController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ErrorStatusMappingTestConfig.class,
        UserCredentialActiveAccountHttpCompatibilityTest.StatusMapperConfiguration.class})
class UserCredentialActiveAccountHttpCompatibilityTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserCredentialIssueUseCase issueUseCase;
    @MockBean UserCredentialQueryUseCase queryUseCase;
    @MockBean UserCredentialRevokeUseCase revokeUseCase;

    @Test void unauthenticatedIs401ForAllPatOperations() throws Exception {
        doThrow(new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated"))
                .when(issueUseCase).issueCredential(org.mockito.ArgumentMatchers.any());
        doThrow(new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated"))
                .when(queryUseCase).getCredentials();
        doThrow(new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated"))
                .when(revokeUseCase).removeCredential(1L);
        assertError(post("/api/auth/pats").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"pat\",\"description\":\"desc\"}"), 401, "AUTH-001", "Unauthenticated");
        assertError(get("/api/auth/pats"), 401, "AUTH-001", "Unauthenticated");
        assertError(delete("/api/auth/pats/1"), 401, "AUTH-001", "Unauthenticated");
    }

    @Test void pendingIs403AndMissingUserIs404() throws Exception {
     doThrow(new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "Access denied"))
             .when(queryUseCase).getCredentials();
     assertError(get("/api/auth/pats"), 403, "AUTH-403", "Access denied");
     doThrow(new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "Access denied"))
             .when(issueUseCase).issueCredential(org.mockito.ArgumentMatchers.any());
     assertError(post("/api/auth/pats").contentType(MediaType.APPLICATION_JSON)
             .content("{\"name\":\"pat\",\"description\":\"desc\"}"), 403, "AUTH-403", "Access denied");
     doThrow(new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "Access denied"))
             .when(revokeUseCase).removeCredential(2L);
     assertError(delete("/api/auth/pats/2"), 403, "AUTH-403", "Access denied");
     doThrow(new UserNotFoundException()).when(issueUseCase).issueCredential(org.mockito.ArgumentMatchers.any());
     assertError(post("/api/auth/pats").contentType(MediaType.APPLICATION_JSON)
             .content("{\"name\":\"pat\",\"description\":\"desc\"}"), 404, "USER-404", "User not found");
     doThrow(new UserNotFoundException()).when(queryUseCase).getCredentials();
     assertError(get("/api/auth/pats"), 404, "USER-404", "User not found");
     doThrow(new UserNotFoundException()).when(revokeUseCase).removeCredential(1L);
     assertError(delete("/api/auth/pats/1"), 404, "USER-404", "User not found");
 }

 @Test void activeSuccessRemainsUnchangedForAllOperations() throws Exception {
     org.mockito.Mockito.when(issueUseCase.issueCredential(org.mockito.ArgumentMatchers.any()))
             .thenReturn(new UserCredentialIssueResult(1L, "token"));
     org.mockito.Mockito.when(queryUseCase.getCredentials()).thenReturn(List.of(
             new UserCredentialSummary(1L, "PAT", "pat", "desc", LocalDateTime.now(), LocalDateTime.now())));
     mockMvc.perform(post("/api/auth/pats").contentType(MediaType.APPLICATION_JSON)
                     .content("{\"name\":\"pat\",\"description\":\"desc\"}"))
             .andExpect(status().isCreated());
     mockMvc.perform(get("/api/auth/pats")).andExpect(status().isOk());
     mockMvc.perform(delete("/api/auth/pats/3")).andExpect(status().isNoContent());
 }

    private void assertError(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                             int status, String code, String message) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value(message))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @TestConfiguration
    @Profile("!test & !identity-access-integration & !change-review-integration")
    static class StatusMapperConfiguration {
        @Bean(name = "credentialStatusMapper") CompositeErrorHttpStatusMapper statusMapper(ApplicationErrorHttpStatusMapper application,
                DomainErrorHttpStatusMapper domain, InfrastructureErrorHttpStatusMapper infrastructure,
                PresentationErrorHttpStatusMapper presentation) {
            return new CompositeErrorHttpStatusMapper(List.of(domain, application, infrastructure, presentation));
        }
    }
}
