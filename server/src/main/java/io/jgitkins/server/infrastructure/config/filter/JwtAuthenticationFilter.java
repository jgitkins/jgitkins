package io.jgitkins.server.infrastructure.config.filter;

import io.jgitkins.server.infrastructure.adapter.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateToken(request, token);
        } catch (JwtException | IllegalArgumentException ex) {
            handleAuthenticationFailure(request, response, new BadCredentialsException("Invalid token", ex));
            return;
        } catch (AuthenticationException ex) {
            handleAuthenticationFailure(request, response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateToken(HttpServletRequest request, String token) {
        Claims claims = jwtService.parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            log.warn("JWT subject is missing");
            throw new BadCredentialsException("Token subject missing");
        }

        UsernamePasswordAuthenticationToken authentication = buildAuthentication(subject, claims);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleAuthenticationFailure(HttpServletRequest request,
                                             HttpServletResponse response,
                                             AuthenticationException exception) throws IOException, ServletException {
        log.warn("JWT authentication failed: {} {}", request.getMethod(), request.getRequestURI());
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, exception);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(String subject, Claims claims) {
        Object rolesClaim = claims.get("roles");
        List<SimpleGrantedAuthority> authorities;
        if (rolesClaim instanceof List<?> roles) {
            authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                    .collect(Collectors.toList());
        } else {
            authorities = Collections.emptyList();
        }
        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }
}
