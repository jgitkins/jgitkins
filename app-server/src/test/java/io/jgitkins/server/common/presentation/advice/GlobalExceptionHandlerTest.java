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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(statusMapper))
                // Turns on Spring 6.1's built-in parameter validation, which is what throws
                // HandlerMethodValidationException. Without it the constrained route below binds
                // happily and the multi-parameter branch stays untested.
                .setValidator(validator)
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

    // --- 응답 본문이 Spring 원문을 흘리지 않는지 (2.115 / 2.116 / 2.121) ---

    /**
     * The guard that keeps {@code MESSAGE_RESOLVERS} and the annotation's type list in step.
     *
     * <p>Three of the six registered types had no branch before this -- ConstraintViolation,
     * MethodArgumentTypeMismatch and MissingServletRequestParameter -- and nothing noticed, because
     * the branch set lived inside a method body where no test could read it. Equality both ways: a
     * type registered without a resolver fails, and so does a resolver for a type nobody registered.
     *
     * <p>What proves the consequence rather than the bookkeeping is
     * {@link #anExceptionWithNothingReportableAnswersTheSafeDefault()} -- the terminal default is a
     * fixed string now, so even a type this guard somehow missed cannot answer with Spring's text.
     */
    @Test
    void everyRegisteredExceptionTypeHasAMessageResolver() throws Exception {
        Method handler = GlobalExceptionHandler.class
                .getDeclaredMethod("handlePresentationException", Exception.class);
        Set<Class<?>> registered = Set.of(handler.getAnnotation(ExceptionHandler.class).value());

        assertThat(GlobalExceptionHandler.MESSAGE_RESOLVERS.keySet())
                .as("a type registered on the handler with no resolver falls to the terminal default, "
                        + "so the caller reads a generic message where a useful one was intended -- and "
                        + "before the default was inverted it read Spring's internals instead")
                .isEqualTo(registered);
    }

    @Test
    void typeMismatchNamesTheParameterAndTheExpectedShape_notTheJavaClass() throws Exception {
        mockMvc.perform(get("/test-errors/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.source").value("presentation"))
                .andExpect(jsonPath("$.error.code").value("REQ-400"))
                // Spring's own text here is "Failed to convert value of type 'java.lang.String' to
                // required type 'java.lang.Long'".
                .andExpect(jsonPath("$.error.message").value("id: expected a number"));
    }

    @Test
    void missingParameterNamesTheParameter_notTheMethodParameterType() throws Exception {
        mockMvc.perform(get("/test-errors/required-parameter"))
                .andExpect(status().isBadRequest())
                // Spring's own text carries "for method parameter type String".
                .andExpect(jsonPath("$.error.message").value("branch: required parameter is missing"));
    }

    /**
     * The branch 2.116 is about, on the path Spring picks for a controller without {@code @Validated}.
     *
     * <p>Names come from {@code MethodParameter#getParameterName}, which needs {@code -parameters} at
     * compile time. The Spring Boot Gradle plugin sets it (verified: the compiled controllers carry a
     * {@code MethodParameters} attribute), and a null name would degrade to the bare message rather
     * than printing "null:" -- so this assertion is also what proves the flag is still on.
     */
    @Test
    void severalConstrainedParametersNameEachOne() throws Exception {
        mockMvc.perform(get("/test-errors/parameters/{namespace}/{repoName}", " ", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message")
                        .value("namespace: must not be blank; repoName: must not be blank"));
    }

    @Test
    void aSingleConstrainedParameterKeepsTheBareMessage() throws Exception {
        mockMvc.perform(get("/test-errors/parameters/{namespace}/{repoName}", " ", "repo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("must not be blank"));
    }

    @Test
    void aSingleViolationKeepsTheBareMessage() throws Exception {
        mockMvc.perform(get("/test-errors/violation-one"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("must not be blank"));
    }

    @Test
    void severalViolationsNameEachField() throws Exception {
        mockMvc.perform(get("/test-errors/violation-many"))
                .andExpect(status().isBadRequest())
                // Sorted by name, because getConstraintViolations() is a Set. Without the sort this
                // assertion would pass or fail depending on iteration order.
                .andExpect(jsonPath("$.error.message")
                        .value("namespace: must not be blank; repoName: must not be blank"));
    }

    @Test
    void anExceptionWithNothingReportableAnswersTheSafeDefault() throws Exception {
        mockMvc.perform(get("/test-errors/violation-none"))
                .andExpect(status().isBadRequest())
                // The old terminal default was ex.getMessage(), which for an empty
                // ConstraintViolationException is Spring's own summary line.
                .andExpect(jsonPath("$.error.message").value("Invalid request"));
    }

    @Test
    void malformedBodyKeepsItsFixedMessage() throws Exception {
        // Regression: this branch existed before and its wording is the idiom the others now follow.
        assertThat(GlobalExceptionHandler.MESSAGE_RESOLVERS)
                .containsKey(org.springframework.http.converter.HttpMessageNotReadableException.class);
    }

    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/test-errors/typed/{id}")
        public ResponseEntity<Void> typed(@PathVariable Long id) {
            return ResponseEntity.ok().build();
        }

        /**
         * Two constrained path variables, which is what makes the multi-error branch reachable.
         *
         * <p>The real case is {@code RepositoryContentController}'s three
         * {@code @PathVariable @NotBlank} strings, but that route is multipart and the request fails
         * during part binding before validation runs, so it cannot demonstrate this.
         */
        @GetMapping("/test-errors/parameters/{namespace}/{repoName}")
        public ResponseEntity<Void> constrainedParameters(@PathVariable @NotBlank String namespace,
                @PathVariable @NotBlank String repoName) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/test-errors/required-parameter")
        public ResponseEntity<Void> requiredParameter(@RequestParam("branch") String branch) {
            return ResponseEntity.ok().build();
        }

        /**
         * {@code ConstraintViolationException} thrown directly rather than through {@code @Validated}.
         *
         * <p>The AOP proxy that produces it for real needs a Spring context, which this test
         * deliberately does not have. What the resolver does with the exception is the same either
         * way, and the real-route half is pinned in {@code BoundaryValidationTest}. Violations come
         * from a real validator so the property paths are real, not hand-built strings.
         */
        @GetMapping("/test-errors/violation-one")
        public ResponseEntity<Void> violationOne() {
            throw new ConstraintViolationException(violationsOf(new OneField()));
        }

        @GetMapping("/test-errors/violation-many")
        public ResponseEntity<Void> violationMany() {
            throw new ConstraintViolationException(violationsOf(new TwoFields()));
        }

        @GetMapping("/test-errors/violation-none")
        public ResponseEntity<Void> violationNone() {
            throw new ConstraintViolationException(Set.of());
        }

        private static <T> Set<ConstraintViolation<T>> violationsOf(T bean) {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                Validator validator = factory.getValidator();
                return validator.validate(bean);
            }
        }

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

    /** One rejection, so the bare-message rule applies. */
    static class OneField {
        @NotBlank
        private String namespace;
    }

    /**
     * Two rejections with the field names the caller used.
     *
     * <p>Named after the real case: {@code RepositoryContentController} takes three
     * {@code @PathVariable @NotBlank} strings, and all three answered "must not be blank" with
     * nothing saying which was which.
     */
    static class TwoFields {
        @NotBlank
        private String namespace;

        @NotBlank
        private String repoName;
    }
}
