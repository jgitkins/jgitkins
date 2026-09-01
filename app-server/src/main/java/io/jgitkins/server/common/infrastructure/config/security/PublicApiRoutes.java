package io.jgitkins.server.common.infrastructure.config.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The routes the api security chain must let an anonymous caller through.
 *
 * <p>One list, two readers. {@code SecurityConfig} turns it into matchers;
 * {@code RouteAuthenticationContractTest} turns it into the strings it compares against the runtime
 * route inventory. They used to be two hand-maintained lists and nothing checked that they agreed --
 * a route could be permitted by the chain and absent from the test's declaration, or the reverse,
 * and both would stay green.
 *
 * <h2>What belongs here, and what belongs in InfraRoutes</h2>
 *
 * <p>If {@code RequestMappingHandlerMapping} enumerates it, it belongs here and to the api chain.
 * Otherwise it belongs to {@link InfraRoutes} and the infra chain. That is the whole rule, and
 * {@code RouteAuthenticationContractTest} enforces it: every mapped route must appear in this list
 * or in the test's PROTECTED set, so a controller route that quietly moved to the infra chain fails
 * the build.
 *
 * <p>It is why the springdoc routes are here rather than with the other documentation paths --
 * springdoc registers them as controller mappings, so the inventory sees them. {@code /swagger-ui/**}
 * (the static webjar tree) is not a mapping, so it lives in {@link InfraRoutes}.
 *
 * <h2>Why the method is a nullable HttpMethod and not a string</h2>
 *
 * <p>{@code /error} accepts any method, and the obvious encoding for that is a string sentinel like
 * {@code "ANY"}. It is a trap. Spring 6's {@code HttpMethod} is a final class, not an enum, so
 * {@code HttpMethod.valueOf("ANY")} does not throw -- it returns an instance named ANY, and the
 * matcher built from it matches no real request. {@code /error} would then fall through to
 * {@code authenticated()}, and because Spring Security 6 filters ERROR dispatches by default, every
 * error response in the application would become a 401 instead of its own status.
 *
 * <p>{@code null} is the value {@code AntPathRequestMatcher} already reads as "any method", so there
 * is no sentinel to parse and no way to get this wrong. {@code "ANY"} exists only as an output of
 * {@link #contractStrings()}.
 */
public final class PublicApiRoutes {

    /** @param method null means every method. */
    public record Route(HttpMethod method, String pattern) {}

    /**
     * Ordered by why each entry is here, not alphabetically, because the reason is the reviewable
     * part. Each one is a route a page app-web serves to a logged-out visitor calls, or the way a
     * caller authenticates in the first place.
     */
    public static final List<Route> ROUTES = List.of(
            // Authenticating. The caller has no token yet, so these cannot require one.
            new Route(HttpMethod.POST, "/api/auth/oauth/login"),

            // /explore, logged out.
            new Route(HttpMethod.GET, "/api/organizes"),
            new Route(HttpMethod.GET, "/api/repositories"),
            new Route(HttpMethod.GET, "/api/users"),

            // /{namespace} and /{namespace}/-/**, logged out. Organization member lists are here
            // because ORGANIZE has no VISIBILITY column while REPOSITORY does: organizations carry no
            // privacy concept, so there is nothing to scope the list by.
            new Route(HttpMethod.GET, "/api/organizes/{organizeId}/members"),

            // /{namespace}/{repoName} and /tree/**, logged out. Each of these authorizes the
            // repository itself in the handler; being here only means the chain does not refuse them.
            new Route(HttpMethod.GET, "/api/internal/repositories/{namespace}/{repoName}/overview"),
            new Route(HttpMethod.GET, "/api/repositories/{repositoryId}/overview"),
            new Route(HttpMethod.GET, "/api/repositories/{namespace}/{repoName}/refs/{branch}/tree"),

            // /{namespace}/{repoName}/find-files/index, logged out.
            new Route(HttpMethod.GET, "/repositories/{namespace}/{repoName}/files/index"),

            // Framework and documentation. Spring's error dispatch has no principal by definition.
            new Route(null, "/error"),
            new Route(HttpMethod.GET, "/swagger-ui.html"),
            new Route(HttpMethod.GET, "/v3/api-docs"),
            new Route(HttpMethod.GET, "/v3/api-docs.yaml"),
            new Route(HttpMethod.GET, "/v3/api-docs/swagger-config"));

    /** What {@code SecurityConfig} passes to {@code requestMatchers(RequestMatcher...)}. */
    public static RequestMatcher[] matchers() {
        return ROUTES.stream()
                .map(route -> route.method() == null
                        ? AntPathRequestMatcher.antMatcher(route.pattern())
                        : AntPathRequestMatcher.antMatcher(route.method(), route.pattern()))
                .toArray(RequestMatcher[]::new);
    }

    /**
     * The same routes in the {@code "VERB /pattern"} shape the route inventory produces.
     *
     * <p>{@code "ANY"} appears only here. It is how a method-less route reads in a failure message,
     * never a value anything parses back.
     */
    public static Set<String> contractStrings() {
        return ROUTES.stream()
                .map(route -> (route.method() == null ? "ANY" : route.method().name()) + " " + route.pattern())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private PublicApiRoutes() {
    }
}
