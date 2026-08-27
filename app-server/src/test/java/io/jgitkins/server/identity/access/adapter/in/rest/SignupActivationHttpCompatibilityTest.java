package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.common.presentation.advice.mapper.ApplicationErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.DomainErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.InfrastructureErrorHttpStatusMapper;
import io.jgitkins.server.common.presentation.advice.mapper.PresentationErrorHttpStatusMapper;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.identity.access.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.identity.access.application.exception.UsernameAlreadyExistsException;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import io.jgitkins.server.identity.access.domain.exception.UserAlreadyActivatedException;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import java.util.List;
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

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CompositeErrorHttpStatusMapper.class,
        ApplicationErrorHttpStatusMapper.class, DomainErrorHttpStatusMapper.class,
        InfrastructureErrorHttpStatusMapper.class, PresentationErrorHttpStatusMapper.class,
        SignupActivationHttpCompatibilityTest.StatusMapperConfiguration.class,
        RequesterUserIdResolver.class})
class SignupActivationHttpCompatibilityTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SignupUseCase signupUseCase;

    @Test
    void validActivationReturnsNullableSuccessEnvelope() throws Exception {
        perform("new_name").andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void missingUsernameReturnsPresentationContract() throws Exception {
        performBody("{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQ-400"))
                .andExpect(jsonPath("$.error.message").value("username is required"))
                .andExpect(jsonPath("$.error.source").value("presentation"));
    }

    @Test
    void invalidPatternUsernameReturnsPresentationContract() throws Exception {
        perform("bad name").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REQ-400"))
                .andExpect(jsonPath("$.error.message").value("Username allows only letters, numbers, dot, hyphen, or underscore."))
                .andExpect(jsonPath("$.error.source").value("presentation"));
    }

    @Test
    void unauthenticatedReturnsApplicationContract() throws Exception {
        doThrow(new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED))
                .when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void missingUserReturnsApplicationContract() throws Exception {
        doThrow(new UserNotFoundException()).when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER-404"))
                .andExpect(jsonPath("$.error.message").value("User not found"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void duplicateUsernameReturnsApplicationContract() throws Exception {
        doThrow(new UsernameAlreadyExistsException("Username already exists"))
                .when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER-409-USERNAME"))
                .andExpect(jsonPath("$.error.message").value("Username already exists"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void namespaceCollisionReturnsApplicationContract() throws Exception {
        doThrow(new OrganizeAlreadyExistsException()).when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ORG-409"))
                .andExpect(jsonPath("$.error.message").value("Namespace already exists"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void ownedRepositoryPreventsRenameWithApplicationContract() throws Exception {
        doThrow(new ApplicationException(ApplicationErrorCode.UNPROCESSABLE,
                "Cannot rename user with existing repositories"))
                .when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("UNPROCESSABLE"))
                .andExpect(jsonPath("$.error.message").value("Cannot rename user with existing repositories"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void alreadyActivatedReturnsDomainContract() throws Exception {
        doThrow(new UserAlreadyActivatedException()).when(signupUseCase).activate(42L, "new_name");
        perform("new_name").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER-409-ACTIVATED"))
                .andExpect(jsonPath("$.error.message").value("User is already activated"))
                .andExpect(jsonPath("$.error.source").value("domain"));
    }

    private org.springframework.test.web.servlet.ResultActions perform(String username) throws Exception {
        return performBody("{\"username\":\"" + username + "\"}");
    }

    private org.springframework.test.web.servlet.ResultActions performBody(String body) throws Exception {
        // Every compatibility case carries a valid principal, so the responses being compared are the
        // ones a real authenticated caller gets. The unauthenticated path is asserted separately below;
        // folding it in here would have every case exercising the 401 instead.
        return mockMvc.perform(post("/api/signup/activate")
                .principal(() -> "42")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void anonymousRequestKeepsTheUnauthenticatedEnvelope() throws Exception {
        // Task 2.63 moved this rejection from the service to the adapter. The status and envelope must
        // not have moved with it -- that is the entire compatibility claim of this task.
        mockMvc.perform(post("/api/signup/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @TestConfiguration
    @Profile("!test & !identity-access-integration & !change-review-integration")
    static class StatusMapperConfiguration {
        @Bean(name = "signupStatusMapper")
        CompositeErrorHttpStatusMapper statusMapper(ApplicationErrorHttpStatusMapper application,
                DomainErrorHttpStatusMapper domain, InfrastructureErrorHttpStatusMapper infrastructure,
                PresentationErrorHttpStatusMapper presentation) {
            return new CompositeErrorHttpStatusMapper(List.of(domain, application, infrastructure, presentation));
        }
    }
}
