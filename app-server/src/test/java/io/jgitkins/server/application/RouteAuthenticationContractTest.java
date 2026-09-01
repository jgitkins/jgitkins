package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import io.jgitkins.server.common.infrastructure.config.security.PublicApiRoutes;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Every mapped route is classified, and a new one breaks the build until it is.
 *
 * <p>Task 2.133's real deliverable is not the one-line change from {@code permitAll} to
 * {@code authenticated}. It is this: five authorization holes (2.111, 2.114, 2.123, 2.125, and the
 * eight reads closed in P0a) all came from the same cause, which is that a route is protected only
 * when whoever wrote it remembered to resolve a requester. Fixing them one at a time leaves the next
 * endpoint free to reproduce the same defect. A list nobody enforces would drift the same way, so
 * the inventory is read out of {@link RequestMappingHandlerMapping} at runtime and compared against
 * a declaration in this file.
 *
 * <h2>What this asserts today, and what it will assert after the flip</h2>
 *
 * <p><strong>Today.</strong> The security chains are still {@code anyRequest().permitAll()}, so the
 * only refusals come from controllers that resolve a requester themselves. Two things are enforceable
 * now and both are enforced: the inventory matches the declaration, and no route on
 * {@link #PUBLIC} answers 401 or 403 to an anonymous caller.
 *
 * <p><strong>After task 2.133 flips the default.</strong> One assertion is added: every route that is
 * not on {@link #PUBLIC} must answer 401 or 404 to an anonymous caller, and never 500. The 500 is the
 * interesting part -- this context runs on an empty H2, so a request that gets past the chain fails
 * on a missing table. "Not 500" is therefore proof the request was refused before it reached the
 * domain, which is a stronger statement than the status code alone.
 *
 * <p><strong>{@link #PUBLIC} is today's fact, not today's aspiration.</strong> Each entry is there
 * because a page app-web serves to a logged-out visitor calls it, or because it is how a caller
 * authenticates in the first place. It was assembled by walking app-web's own permitAll surface
 * ({@code SecurityConfig} plus {@code PublicNamespaceRequestMatcher}) through its facades and ports
 * to {@code JGitkinsServerClient}. Two routes that a first draft listed by guesswork are absent for
 * that reason, and two it had missed are present.
 *
 * <h2>Public does not mean unauthorized</h2>
 *
 * <p>A route on {@link #PUBLIC} is one the security chain must let through. It says nothing about
 * whether the handler then authorizes the resource. Repository visibility is a property of a row, not
 * of a URL, so the repository reads listed here still answer not-found for a repository the caller
 * cannot see -- that decision lives in the controller and is asserted by that route's own tests. The
 * assertion here is only that the chain does not refuse them, which is why it checks for the absence
 * of 401 and 403 rather than the presence of 200.
 *
 * <h2>What this does not cover</h2>
 *
 * <p>The git security chain. {@code RequestMappingHandlerMapping} enumerates controller mappings, and
 * no controller serves {@code /git/**} or {@code /**}{@code /*.git} -- the chain guards paths that
 * nothing is mapped to yet. Task 2.127 covers it separately.
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
class RouteAuthenticationContractTest {

    /**
     * Routes the security chain must let an anonymous caller through.
     *
     * <p>Read from {@link PublicApiRoutes}, the same list {@code SecurityConfig} builds its matchers
     * from. It used to be a second literal here, and nothing checked that the two agreed -- a route
     * the chain permitted could be missing from this declaration, or the reverse, and both stayed
     * green. One list means that class of drift cannot happen.
     *
     * <p><strong>{@code PublicApiRoutes} is today's fact, not today's aspiration.</strong> Each entry
     * is there because a page app-web serves to a logged-out visitor calls it, or because it is how a
     * caller authenticates in the first place. The reasons live next to the entries in that class.
     */
    private static final Set<String> PUBLIC = PublicApiRoutes.contractStrings();

    /**
     * Routes that must require authentication.
     *
     * <p>Declared rather than derived, so that a route added tomorrow lands in neither set and this
     * test fails. That is the whole mechanism: 2.111, 2.114, 2.123, 2.125 and P0a's eight reads were
     * all routes nobody classified, and a list that fills itself in would have missed them the same
     * way.
     *
     * <p>Nothing here is asserted about anonymous behaviour <em>yet</em>. The chains are still
     * {@code permitAll}, so several of these do reach the domain today -- that is precisely the gap
     * task 2.133 closes, and {@link #everyRouteIsClassified} is what keeps the list honest until then.
     */
    private static final Set<String> PROTECTED = new TreeSet<>(List.of(
            "DELETE /api/auth/pats/{credentialId}",
            "DELETE /api/organizes/{organizeId}",
            "DELETE /api/organizes/{organizeId}/members/{userId}",
            "DELETE /api/repositories/{repositoryId}",
            "DELETE /api/repositories/{repositoryId}/branches/{branchName}",
            "DELETE /api/repositories/{repositoryId}/members/{userId}",
            "GET /api/admin/users",
            "GET /api/admin/users/{userId}",
            "GET /api/auth/pats",
            "GET /api/internal/organizes",
            "GET /api/internal/repositories/users/{username}",
            "GET /api/organizes/me",
            "GET /api/organizes/{organizeId}",
            "GET /api/repositories/users/{username}",
            "GET /api/repositories/{repositoryId}",
            "GET /api/repositories/{repositoryId}/branches",
            "GET /api/repositories/{repositoryId}/branches/{branchName}",
            "GET /api/repositories/{repositoryId}/members",
            "GET /repositories/{namespace}/{repoName}/branches/{branch}/commits",
            "GET /repositories/{namespace}/{repoName}/commits/{commitHash}",
            "GET /repositories/{namespace}/{repoName}/files",
            "GET /repositories/{namespace}/{repoName}/merge/check",
            "GET /repositories/{namespace}/{repoName}/pull-requests/{pullRequestId}",
            "PATCH /api/admin/users/{userId}/status",
            "POST /api/auth/pats",
            "POST /api/organizes",
            "POST /api/organizes/{organizeId}/members",
            "POST /api/repositories",
            "POST /api/repositories/{namespace}/{repoName}/files/{branch}",
            "POST /api/repositories/{repositoryId}/branches",
            "POST /api/repositories/{repositoryId}/files",
            "POST /api/repositories/{repositoryId}/members",
            "POST /api/signup/activate",
            "POST /repositories/{namespace}/{repoName}/merge",
            "POST /repositories/{namespace}/{repoName}/pull-requests"));

    /**
     * Runner routes, deliberately unclassified.
     *
     * <p>Runner work is paused, and 2.126 (a fail-open scope check) has to land before these can be
     * judged: classifying them now would bake in an answer nobody has decided. They are excluded from
     * both sets rather than quietly listed as public, so the exclusion is visible.
     */
    private static final Set<String> RUNNER_DEFERRED = Set.of(
            "GET /api/runners",
            "POST /api/runners",
            "GET /api/runners/{runnerId}",
            "DELETE /api/runners/{runnerId}",
            "POST /api/runners/activate");

    /** Writes that legitimately precede authentication. Any other write on PUBLIC is a defect. */
    private static final Set<String> PUBLIC_WRITES_ALLOWED = Set.of("POST /api/auth/oauth/login");

    /**
     * A ceiling on how much of the API may be public.
     *
     * <p>The failure mode of a list like this is not that it goes red. It is that it grows one entry
     * at a time until it covers everything and asserts nothing. Raising this number is a deliberate
     * act that shows up in a diff.
     *
     * <p>Equal to {@link #PUBLIC}'s current size, not comfortably above it. At 18 against 14 entries
     * the first four additions were free, which is the drift this constant exists to catch -- the
     * assertion only bites once the slack runs out, and by then four routes went public without
     * anyone deciding they should.
     */
    private static final int PUBLIC_CEILING = 14;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyRouteIsClassified() {
        Set<String> actual = inventory();
        Set<String> declared = new TreeSet<>(PUBLIC);
        declared.addAll(PROTECTED);
        declared.addAll(RUNNER_DEFERRED);

        Set<String> unclassified = new TreeSet<>(actual);
        unclassified.removeAll(declared);
        Set<String> declaredButGone = new TreeSet<>(declared);
        declaredButGone.removeAll(actual);

        // This is the assertion the whole test exists for. A route added without a decision about who
        // may call it fails here, before it can ship as the sixth instance of the same hole.
        assertThat(unclassified)
                .as("a new route is mapped and nobody has said who may call it. Add it to PUBLIC "
                        + "(the chain lets anonymous through) or PROTECTED (it requires "
                        + "authentication). Reaching for PUBLIC because the test is red is how this "
                        + "list stops meaning anything")
                .isEmpty();

        assertThat(declaredButGone)
                .as("these routes are declared here but no longer mapped; a stale entry describes "
                        + "nothing and misleads whoever reads it next")
                .isEmpty();

        // No overlap: a route cannot be both, and a copy-paste that puts one in two sets would
        // otherwise pass silently.
        Set<String> both = new TreeSet<>(PUBLIC);
        both.retainAll(PROTECTED);
        assertThat(both).as("a route declared both public and protected").isEmpty();
    }

    @Test
    void thePublicListCarriesNoWriteItDidNotEarn() {
        Set<String> writes = new TreeSet<>();
        for (String route : PUBLIC) {
            String verb = route.substring(0, route.indexOf(' '));
            if (List.of("POST", "PUT", "PATCH", "DELETE").contains(verb)
                    && !PUBLIC_WRITES_ALLOWED.contains(route)) {
                writes.add(route);
            }
        }

        assertThat(writes)
                .as("a write reachable without authentication is how 2.111 (anonymous organization "
                        + "delete) and 2.123 (anonymous merge) happened. Adding one here needs a "
                        + "reason in PUBLIC_WRITES_ALLOWED, not a quiet entry in PUBLIC")
                .isEmpty();
    }

    @Test
    void thePublicListStaysSmallerThanTheCeiling() {
        assertThat(PUBLIC)
                .as("PUBLIC has grown past its ceiling. Raising PUBLIC_CEILING is fine when the "
                        + "growth is real; doing it without noticing is how this test stops testing")
                .hasSizeLessThanOrEqualTo(PUBLIC_CEILING);
    }

    @Test
    void noPublicRouteRefusesAnAnonymousCaller() {
        List<String> refused = new ArrayList<>();
        for (String route : PUBLIC) {
            if (route.startsWith("ANY ")) {
                // Spring's error dispatch is not reachable as a normal request.
                continue;
            }
            int space = route.indexOf(' ');
            HttpMethod method = HttpMethod.valueOf(route.substring(0, space));
            String uri = concreteUri(route.substring(space + 1));

            int status;
            try {
                MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.request(method, uri)).andReturn();
                status = result.getResponse().getStatus();
            } catch (Exception e) {
                // A handler that throws on an empty H2 is not a chain refusal, which is what this
                // test is about.
                continue;
            }
            if (status == 401 || status == 403) {
                refused.add(route + " -> " + status);
            }
        }

        assertThat(refused)
                .as("these routes are called by pages app-web serves to logged-out visitors. A 401 "
                        + "or 403 here is not a test failure to be waved through -- it is the "
                        + "anonymous half of the site being broken")
                .isEmpty();
    }

    /**
     * Fills path variables with values that parse.
     *
     * <p>The values do not have to exist. Both the chain refusal this test looks for and the 401 the
     * flip will add happen before the handler runs, so a repository id of 1 is enough; asking for a
     * real one would make the test depend on fixtures it has no reason to care about.
     */
    private static String concreteUri(String pattern) {
        return pattern
                .replace("{repositoryId}", "1")
                .replace("{organizeId}", "1")
                .replace("{userId}", "1")
                .replace("{credentialId}", "1")
                .replace("{pullRequestId}", "1")
                .replace("{runnerId}", "1")
                .replace("{namespace}", "ns")
                .replace("{repoName}", "repo")
                .replace("{username}", "someone")
                .replace("{branch}", "main")
                .replace("{branchName}", "main")
                .replace("{commitHash}", "0000000000000000000000000000000000000000");
    }

    private Set<String> inventory() {
        Set<String> rows = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((info, method) -> {
            for (String pattern : patterns(info)) {
                for (String verb : verbs(info)) {
                    rows.add(verb + " " + pattern);
                }
            }
        });
        return rows;
    }

    private static List<String> patterns(RequestMappingInfo info) {
        List<String> out = new ArrayList<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatternValues().forEach(out::add);
        } else if (info.getPatternsCondition() != null) {
            out.addAll(info.getPatternsCondition().getPatterns());
        }
        return out;
    }

    private static List<String> verbs(RequestMappingInfo info) {
        List<String> out = new ArrayList<>();
        info.getMethodsCondition().getMethods().forEach(m -> out.add(m.name()));
        if (out.isEmpty()) {
            out.add("ANY");
        }
        return out;
    }
}
