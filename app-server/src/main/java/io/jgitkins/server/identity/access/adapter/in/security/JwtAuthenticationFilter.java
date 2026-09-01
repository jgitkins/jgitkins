package io.jgitkins.server.identity.access.adapter.in.security;

import io.jgitkins.server.common.infrastructure.config.security.handler.ApiAnauthorizeHandler;
import io.jgitkins.server.identity.access.application.dto.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthService jwtAuthService;
    private final ApiAnauthorizeHandler apiAnauthorizeHandler;

    /**
     * {@code /actuator/} is deliberately absent.
     *
     * <p>It used to be listed, which made two endpoints unreachable by anyone rather than protected.
     * {@code management.endpoints.web.exposure.include} publishes health, info and prometheus;
     * {@code InfraRoutes} opens only prometheus, so health and info fall to the api chain and take
     * {@code authenticated()} after 8eb64b5. Skipping this filter for them meant no credential could
     * ever be established, so they answered 401 to an operator holding a valid JWT -- fail-closed, but
     * a brick rather than a lock, and {@code everyActuatorEndpointIsClassified} could not tell the two
     * apart because it only ever called them anonymously.
     *
     * <p>Prometheus is unaffected either way: the infra chain matches it at {@code @Order(2)} and
     * permits it, and an anonymous request here clears the context and continues regardless.
     *
     * <p>The remaining four are on the infra chain or are permitted api routes, and they authenticate
     * by mechanisms this filter does not serve -- an authorization-code redirect, a static docs asset.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/oauth2/") || path.startsWith("/login/")
                || path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            // This chain authenticates with a Bearer JWT and nothing else, so a request without one
            // must reach the controllers unauthenticated rather than carrying whatever the session
            // repository happened to load. A browser that completed the OAuth handshake still has an
            // OAuth2AuthenticationToken in its session, and @AuthenticationPrincipal(expression =
            // "username") evaluates that expression against the principal without guarding the call:
            // a DefaultOidcUser has no such property, so every route that reads the requester answered
            // 500. The JWT is the credential for these routes -- OAuth2LoginSuccessHandler hands it to
            // the client as OAuthLoginResult#appToken -- which is why dropping the session principal
            // here loses no authentication that this chain would have honored.
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        var result = jwtAuthService.authenticate(token);
        if (result.isEmpty()) {
            apiAnauthorizeHandler.commence(request, response, null);
            return;
        }
        JwtAuthenticationResult authenticationResult = result.get();
        List<SimpleGrantedAuthority> authorities = authenticationResult.roles().stream()
                .map(SimpleGrantedAuthority::new).toList();
        // AuthenticatedUser, not Spring's User wrapping the id as a username. The principal is now a
        // type controllers can declare, which is what removes the expression = "username" round trip
        // and the two 500s it caused. AuthenticatedUser implements Principal, so
        // authentication.getName() still answers the numeric id for the three consumers that parse it.
        var auth = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(authenticationResult.userId()), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
