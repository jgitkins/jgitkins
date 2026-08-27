package io.jgitkins.server.identity.access.adapter.in.security;

import io.jgitkins.server.common.infrastructure.config.security.handler.ApiAnauthorizeHandler;
import io.jgitkins.server.identity.access.application.dto.result.JwtAuthenticationResult;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtAuthService jwtAuthService;
    private final ApiAnauthorizeHandler apiAnauthorizeHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/oauth2/") || path.startsWith("/login/")
                || path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/"));
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
        var auth = new UsernamePasswordAuthenticationToken(
                new User(String.valueOf(authenticationResult.userId()), "", authorities), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
