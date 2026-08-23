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
