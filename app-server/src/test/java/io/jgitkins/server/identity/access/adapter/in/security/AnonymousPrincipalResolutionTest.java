package io.jgitkins.server.identity.access.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Guards the anonymous-allowed reads against the defect fixed alongside this test: every one of these
 * routes answered 500 to an anonymous caller.
 *
 * <p>{@code AnonymousAuthenticationFilter} sets the principal to the String {@code "anonymousUser"},
 * and {@code AuthenticationPrincipalArgumentResolver} evaluates {@code expression = "username"} against
 * that principal without guarding the call, so argument resolution threw
 * {@code SpelEvaluationException EL1008E} before any controller body ran. The fix disables anonymous on
 * the API chain, which makes the authentication null and sends the resolver down its early return.
 *
 * <p>This test runs the REAL filter chain on purpose. It found nothing for as long as it did not exist
 * because every other test over these routes uses {@code addFilters = false} or {@code standaloneSetup},
 * which leaves {@code SecurityContextHolder} empty and never evaluates the expression at all.
 *
 * <p>The assertion is deliberately about argument resolution rather than about status codes: the test
 * context runs on an empty H2, so the data-dependent routes still answer 500 from a missing table. That
 * is environmental. What must never come back is a failure that happens before the controller is called.
 */
// The gRPC server autoconfigurations are excluded, matching OutboundAdapterSpringContextTest.
// @AutoConfigureMockMvc gives this class its own context cache key, so a second application context
// coexists with the shared one and both would bind the fixed gRPC port. Setting grpc.server.port=0
// instead does not work: net.devh 2.15 resolves a random port through Spring's SocketUtils, which
// Spring 6 removed, so the context then fails with NoClassDefFoundError. Nothing here needs gRPC.
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
class AnonymousPrincipalResolutionTest {

    /** Reads whose contract allows an anonymous caller — the {@code orElse(null)} requester sites. */
    private static final List<String> ANONYMOUS_ALLOWED_READS = List.of(
            "/api/organizes/me",
            "/api/repositories",
            "/api/repositories/1",
            "/api/repositories/users/someone",
            "/api/repositories/1/overview",
            "/api/internal/organizes",
            "/api/internal/repositories/users/someone",
            "/api/internal/repositories/ns/repo/overview");

    /** Routes among the above that need no table, so anonymous access is observable end to end. */
    private static final List<String> SCHEMA_FREE_READS = List.of(
            "/api/organizes/me",
            "/api/internal/organizes");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCallerResolvesRequesterInsteadOfFailingArgumentResolution() throws Exception {
        for (String route : ANONYMOUS_ALLOWED_READS) {
            MvcResult result = mockMvc.perform(get(route)).andReturn();
            assertThat(findSpelFailure(result.getResolvedException()))
                    .as("argument resolution must not fail for anonymous caller on %s", route)
                    .isNull();
        }
    }

    @Test
    void anonymousCallerGetsASuccessfulResponseWhereNoTableIsNeeded() throws Exception {
        for (String route : SCHEMA_FREE_READS) {
            MvcResult result = mockMvc.perform(get(route)).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("anonymous read %s", route)
                    .isEqualTo(200);
        }
    }

    // The negative control that used to live here injected the String anonymous principal back into
    // the context and asserted the SpelEvaluationException returned. It is gone because the mechanism
    // can no longer be reproduced through this chain: JwtAuthenticationFilter clears the context on a
    // request without a Bearer header, so no foreign principal reaches the argument resolver at all.
    // Both fixes were confirmed non-vacuous by removing them and watching these tests fail -- recorded
    // in the commits that added them. OAuth2SessionPrincipalResolutionTest carries the Bearer happy
    // path, which is what proves clearing the context did not break real authentication.

    private static SpelEvaluationException findSpelFailure(Throwable thrown) {
        Throwable walk = thrown;
        while (walk != null) {
            if (walk instanceof SpelEvaluationException spel) {
                return spel;
            }
            if (walk.getCause() == walk) {
                return null;
            }
            walk = walk.getCause();
        }
        return null;
    }
}
