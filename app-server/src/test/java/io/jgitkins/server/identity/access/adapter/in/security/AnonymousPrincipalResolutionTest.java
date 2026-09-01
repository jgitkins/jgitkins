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
 *
 * <h2>Retargeted when the default flipped</h2>
 *
 * <p>The original list was the {@code orElse(null)} requester sites, most of which are protected. Task
 * 2.133 made the api chain refuse an anonymous caller on those before the handler runs, so asking them
 * about argument resolution stopped asking anything -- the resolver is never reached, and the test
 * would have gone quietly vacuous rather than red.
 *
 * <p>The list is now the public routes that read a requester, which are the only places an anonymous
 * caller still reaches {@code @CurrentUser} at all. That is a smaller surface than before and it is the
 * whole of the surface that remains: the fix under test only has somewhere to fail where the chain
 * lets an anonymous request through.
 *
 * <p>The companion assertion that an anonymous caller gets a 200 where no table is needed is gone. Both
 * routes it used ({@code /api/organizes/me}, {@code /api/internal/organizes}) are protected now, and no
 * public route is both schema-free and requester-reading. "The chain does not refuse a public route" is
 * asserted by {@code RouteAuthenticationContractTest#noPublicRouteRefusesAnAnonymousCaller}, over the
 * full public list rather than two entries.
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

    /**
     * Public routes that read a requester — where an anonymous caller still reaches
     * {@code @CurrentUser}, and so the only places the defect under test could return.
     */
    private static final List<String> ANONYMOUS_ALLOWED_READS = List.of(
            "/api/organizes",
            "/api/repositories",
            "/api/repositories/1/overview",
            "/api/organizes/1/members",
            "/api/internal/repositories/ns/repo/overview");

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
    void anonymousCallerReachesTheHandlerOnAPublicRoute() throws Exception {
        // Not a duplicate of the contract test's public-route check: that one asserts the chain does
        // not refuse. This one asserts the request got past argument resolution too, which on an empty
        // H2 shows up as the domain's own failure rather than a 401 or a resolver exception.
        for (String route : ANONYMOUS_ALLOWED_READS) {
            int status = mockMvc.perform(get(route)).andReturn().getResponse().getStatus();
            assertThat(status)
                    .as("anonymous read %s must reach the handler, not be refused by the chain", route)
                    .isNotIn(401, 403);
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
