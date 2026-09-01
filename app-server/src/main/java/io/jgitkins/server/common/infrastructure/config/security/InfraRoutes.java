package io.jgitkins.server.common.infrastructure.config.security;

import java.util.List;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The paths the infra security chain owns: framework surfaces that no controller serves.
 *
 * <p>The rule that decides membership is the inverse of {@link PublicApiRoutes}'s: if
 * {@code RequestMappingHandlerMapping} does not enumerate it, it belongs here. Actuator endpoints
 * come from {@code WebMvcEndpointHandlerMapping}, {@code /oauth2/**} and {@code /login/**} are
 * served by Spring Security's own filters, and {@code /swagger-ui/**} is a static webjar tree. None
 * of them appear in the controller inventory, which is exactly why they need their own chain --
 * mixed into the api chain's public list they would be invisible to the guard that keeps that list
 * honest.
 *
 * <h2>/actuator/prometheus, not /actuator/**</h2>
 *
 * <p>{@code management.endpoints.web.exposure.include} is {@code health,info,prometheus}. Matching
 * the whole tree would carry health and info into this chain and open them. Naming one path leaves
 * the other two unmatched, so they fall through to the api chain and become {@code authenticated()}
 * with no extra rule. app-web already made the same call: its {@code SecurityConfig} opens
 * {@code /actuator/prometheus} alone.
 *
 * <h2>Why this chain must carry a securityMatcher</h2>
 *
 * <p>A {@code SecurityFilterChain} with no {@code securityMatcher} uses {@code AnyRequestMatcher}.
 * Registering this one at {@code @Order(2)} without {@link #MATCHER} would make it the catch-all and
 * leave the api chain at {@code @Order(3)} unable to receive a single request -- silently: the
 * application starts, the JWT filter never runs, and every request is judged by the rules here.
 */
public final class InfraRoutes {

    public static final List<String> PATTERNS = List.of(
            "/actuator/prometheus",
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**");

    public static final RequestMatcher MATCHER = new OrRequestMatcher(
            PATTERNS.stream().map(AntPathRequestMatcher::antMatcher).map(RequestMatcher.class::cast).toList());

    private InfraRoutes() {
    }
}
