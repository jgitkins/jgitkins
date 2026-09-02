package io.jgitkins.server.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
 *
 * <h2>Why every request here carries a Bearer token</h2>
 *
 * <p>Task 2.133 flipped the api chain's default to {@code authenticated()}. On a protected route the
 * chain now answers 401 <em>before</em> the validator runs, so an unauthenticated request can no
 * longer observe boundary validation at all -- these tests all went red with 401 the moment the
 * default flipped, not because validation broke but because they never reached it.
 *
 * <p>That ordering is correct and worth stating: on a protected route, 401 pre-empts 400, 404 and
 * 405. An anonymous caller does not get to learn which of its other mistakes the server would have
 * objected to, or whether the path it typed exists. The boundary guarantees this class protects are
 * therefore guarantees for <em>authenticated</em> callers, which is what the token below makes
 * observable.
 *
 * <p>The token is issued through {@code TokenIssuerPort} and presented as a real
 * {@code Authorization: Bearer} header, so {@code JwtAuthenticationFilter} runs for real.
 * Putting an {@code Authentication} straight into {@code SecurityContextHolder} would not work here:
 * this class runs the real filter chain, and {@code SecurityContextHolderFilter} replaces the context
 * at the start of every request. {@code /api/auth/oauth/login} is public and ignores the header.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
        + "net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration"})
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
                    "field", "branchName"),
            // The route this test's javadoc was written about. Task 2.94 put @Valid on
            // OAuthLoginController and the record it validates carried no constraints at all, so the
            // annotation checked nothing while looking like it did -- exactly the failure mode
            // described above, shipped in the same task that was meant to prevent it.
            Map.of("route", "/api/auth/oauth/login",
                    "body", "{\"provider\":\"\",\"idToken\":\"t\"}",
                    "field", "provider"),
            Map.of("route", "/api/auth/oauth/login",
                    "body", "{\"provider\":\"google\"}",
                    "field", "idToken"));

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TokenIssuerPort tokenIssuerPort;

    private MockMvc mockMvc;

    /**
     * Built here rather than injected so every request carries the Bearer token by default.
     *
     * <p>{@code defaultRequest} only supplies headers the individual request has not already set, so
     * the one test that issues its own token keeps it and nothing is sent twice.
     */
    @BeforeEach
    void authenticateEveryRequest() {
        String token = tokenIssuerPort.issueToken(1L, List.of("ROLE_USER"));
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
                        .springSecurity())
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .build();
    }

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
     * The path-variable half, on a route that can actually reach it.
     *
     * <p>Spring 6.1 enables built-in parameter validation only when the class is NOT annotated
     * {@code @Validated}; that path throws {@code HandlerMethodValidationException}, while a
     * {@code @Validated} class goes through AOP and throws {@code ConstraintViolationException}.
     * {@code GlobalExceptionHandler} covered the second and not the first, so the built-in path fell to
     * the framework's own {@code ErrorResponse} handling: 400, but with an empty body. A caller got a
     * status and no error code, no message, no field name, while every other error on this API carries
     * the {@code ApiResponse} envelope.
     *
     * <p>{@code MergeController} is used because it has no {@code @Validated} and its check route is a
     * plain GET. {@code RepositoryContentController} also lacks {@code @Validated} and has three
     * constrained path variables, but its route is multipart and the request fails during part binding
     * before validation runs, so it cannot demonstrate this.
     */
    @Test
    void aBlankPathSegmentIsRefusedWithTheSameEnvelopeAsEveryOtherError() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/repositories/{namespace}/{repoName}/merge/check", " ", "repo")
                                .param("sourceBranch", "feature")
                                .param("targetBranch", "main"))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("a blank path segment is refused at the boundary")
                .isEqualTo(400);
        assertThat(result.getResponse().getContentAsString())
                .as("and carries the ApiResponse envelope rather than an empty body")
                .contains("\"code\"")
                .contains("must not be blank");
    }

    /**
     * Every standard Spring MVC client error answers with the status that names it.
     *
     * <p>Found by measuring rather than predicting. The task's measurement step, meant to size how much
     * still reached a domain invariant after the boundary constraints went in, answered that: nothing
     * did, nine of nine refused at 400. But one probe used the wrong HTTP method by accident and came
     * back 500, which turned into a sweep of the standard client errors. Three of six answered 500:
     * wrong method, unsupported content type, and a missing required query parameter. All three are
     * entirely the caller's mistake, and all three were waking whoever watches the 5xx rate.
     */
    @Test
    void everyStandardClientMistakeAnswersItsOwnStatus() throws Exception {
        assertThat(mockMvc.perform(post("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus())
                .as("wrong HTTP method").isEqualTo(405);

        assertThat(mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andReturn().getResponse().getStatus())
                .as("unsupported content type").isEqualTo(415);

        assertThat(mockMvc.perform(get("/repositories/ns/repo/merge/check"))
                .andReturn().getResponse().getStatus())
                .as("missing required query parameter").isEqualTo(400);

        assertThat(mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andReturn().getResponse().getStatus())
                .as("malformed json").isEqualTo(400);

        assertThat(mockMvc.perform(get("/api/repositories/not-a-number"))
                .andReturn().getResponse().getStatus())
                .as("path variable type mismatch").isEqualTo(400);
    }

    /**
     * An unmatched path is the caller's typo, not a server failure.
     *
     * <p>{@code GlobalExceptionHandler} listed {@code NoHandlerFoundException}, but Spring 6 throws
     * {@code NoResourceFoundException} for an unmatched path, so every mistyped URL fell to the
     * {@code Exception} catch-all and answered 500 INTERNAL_ERROR. Found while building the
     * path-variable test above, whose first URL was wrong.
     */
    @Test
    void anUnmatchedPathIsNotAServerError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/there-is-no-such-route")).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("a mistyped URL is a client error")
                .isEqualTo(404);
        assertThat(result.getResponse().getContentAsString())
                .as("and its code names the status rather than reusing REQ-400")
                .contains("REQ-404");
    }

    /**
     * A non-positive id in the path is the caller's, and must not answer 500.
     *
     * <p>The body constraints do not cover path variables, so before this an authenticated
     * {@code POST /api/organizes/0/members} reached {@code OrganizeOwnerId.of(0)} and threw
     * {@code IllegalArgumentException}, which has no handler and fell to the catch-all. Measured, not
     * assumed: the same probe unauthenticated answered 401, which is why the token is minted here.
     */
    @Test
    void aNonPositiveIdInThePathIsRefused() throws Exception {
        String token = tokenIssuerPort.issueToken(42L, List.of("ROLE_USER"));
        for (String route : List.of("/api/organizes/0/members", "/api/repositories/0/branches",
                "/api/repositories/-1/members")) {
            MvcResult r = mockMvc.perform(post(route)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":5,\"branchName\":\"x\"}"))
                    .andReturn();

            assertThat(r.getResponse().getStatus())
                    .as("%s must be refused at the boundary rather than reaching an identifier value "
                            + "object and answering 500", route)
                    .isEqualTo(400);
        }
    }

    /**
     * A request with several bad fields is told about all of them.
     *
     * <p>Task 2.99. The handler returned {@code getFieldError()}, the first one, which nobody noticed
     * while five DTOs carried constraints. Task 2.94 put them on twelve more, so a caller with three
     * bad fields would have been fixing them one round trip at a time.
     *
     * <p>A single error still returns the bare constraint message. That is what the HTTP compatibility
     * tests pin, and prefixing it would read "username: username is required".
     */
    @Test
    void everyBadFieldIsNamed() throws Exception {
        // Two bad fields at once: a blank name and a pattern violation cannot both be reported by a
        // handler that returns only the first.
        MvcResult result = mockMvc.perform(post("/repositories/ns/repo/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceBranch\":\"\",\"targetBranch\":\"\"}"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .as("the built-in path aggregates path variables and body together, so both bad "
                        + "branches are named")
                .contains("sourceBranch").contains("targetBranch");

        // The body path, which throws a different exception and has its own aggregation.
        MvcResult sameFieldTwice = mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andReturn();
        assertThat(sameFieldTwice.getResponse().getContentAsString())
                .as("a field that breaks two constraints reports both")
                .contains("must not be blank")
                .contains("alphanumeric");
    }

    /**
     * A body we could not parse says so, without Jackson's internals.
     *
     * <p>The fallback returned {@code ex.getMessage()}, which for a parse failure carries class names
     * and JSON pointers into the response body.
     */
    @Test
    void aMalformedBodyDoesNotLeakParserInternals() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Malformed request body");
        assertThat(body)
                .as("no parser internals in the response")
                .doesNotContain("JSON parse error")
                .doesNotContain("com.fasterxml");
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
