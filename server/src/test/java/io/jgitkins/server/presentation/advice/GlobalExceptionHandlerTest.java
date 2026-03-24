package io.jgitkins.server.presentation.advice;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.error.DomainErrorCode;
import io.jgitkins.server.domain.exception.DomainException;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.presentation.advice.mapper.*;
import io.jgitkins.server.presentation.common.error.PresentationErrorCode;
import io.jgitkins.server.presentation.exception.PresentationException;
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
    void returns422WithSourceDomain_whenUserAlreadyActivated() throws Exception {
        mockMvc.perform(get("/test-errors/domain-conflict"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.source").value("domain"))
                .andExpect(jsonPath("$.error.code").value("USERNAME_ALREADY_SET")); // DomainErrorCode.USER_ALREADY_ACTIVATED.getCode()
    }

    // --- ApplicationException 시나리오 ---

    @Test
    void returns401WithSourceApplication_whenAccessDenied() throws Exception {
        mockMvc.perform(get("/test-errors/application-unauthorized"))
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.message").value("Unauthenticated"));
    }

    @Test
    void returns403WithSourceApplication_whenForbidden() throws Exception {
        mockMvc.perform(get("/test-errors/application-forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void returns404WithSourceApplication_whenResourceNotFound() throws Exception {
        mockMvc.perform(get("/test-errors/application-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.source").value("application"))
                .andExpect(jsonPath("$.error.code").value("REPOSITORY_NOT_FOUND"));
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
        // JgitkinsException fallback 경로 검증
        // inferSourceFallback: PresentationErrorCode class name starts with
        // "Presentation" → source="presentation"
        mockMvc.perform(get("/test-errors/presentation-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.source").value("presentation"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/test-errors/domain")
        public ResponseEntity<Void> domain() {
            throw new DomainException(DomainErrorCode.RULE_VIOLATION, "domain rule violated");
        }

        @GetMapping("/test-errors/domain-conflict")
        public ResponseEntity<Void> domainConflict() {
            throw new DomainException(DomainErrorCode.USER_ALREADY_ACTIVATED, "already activated");
        }

        @GetMapping("/test-errors/application-unauthorized")
        public ResponseEntity<Void> applicationUnauthorized() {
            throw new ApplicationException(ApplicationErrorCode.ACCESS_DENIED, "Unauthenticated");
        }

        @GetMapping("/test-errors/application-forbidden")
        public ResponseEntity<Void> applicationForbidden() {
            throw new ApplicationException(ApplicationErrorCode.ACCESS_DENIED, "not allowed");
        }

        @GetMapping("/test-errors/application-not-found")
        public ResponseEntity<Void> applicationNotFound() {
            throw new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND, "repo missing");
        }

        @GetMapping("/test-errors/infrastructure")
        public ResponseEntity<Void> infrastructure() {
            throw new InfrastructureException(InfrastructureErrorCode.INTERNAL_ERROR, "DB connection failed");
        }

        @GetMapping("/test-errors/presentation-unauthorized")
        public ResponseEntity<Void> presentationUnauthorized() {
            // JgitkinsException fallback 경로 검증
            throw new PresentationException(PresentationErrorCode.UNAUTHORIZED,
                    "token missing");
        }
    }
}
