package io.jgitkins.server.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Proves the boundary constraints added by task 2.94 actually reject, against the real filter chain
 * and the real validator.
 *
 * <p>A green suite does not prove validation is on. A constraint annotation with no {@code @Valid}, or
 * a {@code @Valid} on a DTO with no constraints, both compile and both pass every existing test while
 * validating nothing. Task 2.94 exists because exactly that state had been shipped: five DTOs carried
 * constraints and five call sites carried {@code @Valid}, and they were the same five.
 *
 * <p>Asserted as "not 500" rather than "exactly 400" for the routes whose handlers need data: this
 * context runs on an empty H2, so a request that gets past validation fails on a missing table. The
 * distinction that matters is whether the request was refused at the boundary or reached the domain.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BoundaryValidationTest {

    /** route, body that violates a constraint, and the field the constraint names. */
    private static final List<Map<String, String>> VIOLATIONS = List.of(
            Map.of("route", "/api/organizes",
                    "body", "{\"name\":\"\"}",
                    "field", "name"),
            Map.of("route", "/api/organizes",
                    "body", "{\"name\":\"has space\"}",
                    "field", "name"),
            Map.of("route", "/api/organizes/1/members",
                    "body", "{\"userId\":null}",
                    "field", "userId"),
            Map.of("route", "/api/organizes/1/members",
                    "body", "{\"userId\":0}",
                    "field", "userId"),
            Map.of("route", "/api/repositories/1/members",
                    "body", "{\"userId\":-1}",
                    "field", "userId"),
            Map.of("route", "/api/repositories/1/branches",
                    "body", "{\"branchName\":\"\"}",
                    "field", "branchName"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyConstrainedFieldIsRefusedAtTheBoundary() throws Exception {
        for (Map<String, String> violation : VIOLATIONS) {
            MvcResult result = mockMvc.perform(post(violation.get("route"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(violation.get("body")))
                    .andReturn();

            assertThat(result.getResponse().getStatus())
                    .as("%s with %s must be refused at the boundary, not answered 500 from the domain",
                            violation.get("route"), violation.get("body"))
                    .isEqualTo(400);
        }
    }

    /**
     * The complement: a field the codebase deliberately leaves optional must still get through. Without
     * this, "everything is rejected" would also pass the test above.
     */
    @Test
    void anOptionalFieldIsNotRefused() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/organizes/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5}"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("a missing role is valid and must not be refused as a validation error")
                .isNotEqualTo(400);
    }
}
