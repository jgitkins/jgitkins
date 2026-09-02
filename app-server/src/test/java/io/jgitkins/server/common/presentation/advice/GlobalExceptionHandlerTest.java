package io.jgitkins.server.common.presentation.advice;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.shared.domain.error.DomainErrorCode;
import io.jgitkins.server.shared.domain.error.DomainProblemSpec;
import io.jgitkins.server.shared.domain.exception.DomainException;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.common.presentation.advice.translator.*;
import io.jgitkins.server.common.presentation.error.PresentationProblemSpec;
import io.jgitkins.server.common.presentation.exception.PresentationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CompositeErrorHttpStatusMapper statusMapper = new CompositeErrorHttpStatusMapper(
                List.of(
                        new DomainErrorHttpStatusMapper(),
                        new ApplicationErrorHttpStatusMapper(),
                        new InfrastructureErrorHttpStatusMapper(),
                        new PresentationErrorHttpStatusMapper()));
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(statusMapper))
                .build();
    }

    // --- DomainException 시나리오 ---

    @Test
    void returns400WithSourceDomain_whenDomainExceptionThrown() throws Exception {
        mockMvc.perform(get("/test-errors/domain"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.source").value("domain"));
    }

    @Test
    void returns409WithSourceDomain_whenUserAlreadyActivated() throws Exception {
        mockMvc.perform(get("/test-errors/domain-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.source").value("domain"))
                .andExpect(jsonPath("$.error.code").value("USER-409-ACTIVATED"));
    }

    // --- ApplicationException 시나리오 ---

    @Test
    void returns401WithSourceApplication_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/test-errors/application-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.message").value("Unauthenticated"));
    }

    @Test
    void returns403WithSourceApplication_whenForbidden() throws Exception {
        mockMvc.perform(get("/test-errors/application-forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.code").value("AUTH-403"));
    }

    @Test
    void returns404WithSourceApplication_whenResourceNotFound() throws Exception {
        mockMvc.perform(get("/test-errors/application-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.code").value("REPO-404"));
    }

    // --- InfrastructureException 시나리오 ---

    @Test
    void returns500WithSourceInfrastructure_whenInfrastructureExceptionThrown() throws Exception {
        mockMvc.perform(get("/test-errors/infrastructure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.source").value("infrastructure"))
                .andExpect(jsonPath("$.error.message").value("DB connection failed"));
    }

    // --- Presentation (Spring MVC) 시나리오는 기존 핸들러에서 처리 ---

    @Test
    void returns401WithSourcePresentation_whenUnauthorizedPresentationCode() throws Exception {
        mockMvc.perform(get("/test-errors/presentation-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.source").value("presentation"))
                .andExpect(jsonPath("$.error.code").value("REQ-401"));
    }

    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/test-errors/domain")
        public ResponseEntity<Void> domain() {
            throw new DomainException(DomainErrorCode.RULE_VIOLATION, "domain rule violated");
        }

        @GetMapping("/test-errors/domain-conflict")
        public ResponseEntity<Void> domainConflict() {
            throw new DomainException(DomainProblemSpec.USER_ALREADY_ACTIVATED, "already activated");
        }

        @GetMapping("/test-errors/application-unauthorized")
        public ResponseEntity<Void> applicationUnauthorized() {
            throw new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated");
        }

        @GetMapping("/test-errors/application-forbidden")
        public ResponseEntity<Void> applicationForbidden() {
            throw new ApplicationException(ApplicationProblemSpec.ACCESS_DENIED, "not allowed");
        }

        @GetMapping("/test-errors/application-not-found")
        public ResponseEntity<Void> applicationNotFound() {
            throw new RepositoryNotFoundException("repo missing");
        }

        @GetMapping("/test-errors/infrastructure")
        public ResponseEntity<Void> infrastructure() {
            throw new InfrastructureException(InfrastructureErrorCode.INTERNAL_ERROR, "DB connection failed");
        }

        @GetMapping("/test-errors/presentation-unauthorized")
        public ResponseEntity<Void> presentationUnauthorized() {
            throw new PresentationException(PresentationProblemSpec.UNAUTHORIZED,
                    "token missing");
        }
    }
}
