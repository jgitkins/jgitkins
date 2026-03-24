package io.jgitkins.server.infrastructure.config.git;

import io.jgitkins.server.infrastructure.config.security.auth.PatTokenAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitRequestAuthSupport {

    private final PatTokenAuthenticationService patTokenAuthenticationService;

    public Long resolveUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return authenticateFromBasic(request);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long authenticateFromBasic(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            log.debug("git basic auth missing or invalid header. uri=[{}]", request.getRequestURI());
            return null;
        }
        String base64 = header.substring("Basic ".length());
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            log.warn("git basic auth decode failed. uri=[{}] reason=[{}]", request.getRequestURI(), ex.getMessage());
            return null;
        }
        int idx = decoded.indexOf(':');
        if (idx <= 0) {
            log.warn("git basic auth malformed credentials payload. uri=[{}]", request.getRequestURI());
            return null;
        }
        String username = decoded.substring(0, idx);
        String password = decoded.substring(idx + 1);
        try {
            Authentication auth = patTokenAuthenticationService.authenticate(username, password);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return Long.valueOf(auth.getName());
        } catch (AuthenticationException | NumberFormatException ex) {
            log.warn("git basic auth failed. uri=[{}] username=[{}] reason=[{}]",
                    request.getRequestURI(),
                    username,
                    ex.getMessage());
            return null;
        }
    }
}
