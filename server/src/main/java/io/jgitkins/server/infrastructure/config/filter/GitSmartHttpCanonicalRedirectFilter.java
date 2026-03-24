package io.jgitkins.server.infrastructure.config.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

public class GitSmartHttpCanonicalRedirectFilter extends OncePerRequestFilter {

    private static final Pattern ROOT_GIT_PATH = Pattern.compile("^/[^/]+/[^/]+\\.git(?:/.*)?$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.FORWARD) {
            return true;
        }
        String path = normalizeRequestPath(request);
        return !shouldCanonicalize(path);
    }

    // Smart HTTP 요청이 들어왔을때 실제 Path 로 Redirect 하기 위한 필터
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = normalizeRequestPath(request);
        String target = buildRedirectTarget(path, request.getQueryString());
        response.setStatus(308);
        response.setHeader("Location", target);
    }

    private String normalizeRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }

    private boolean shouldCanonicalize(String path) {
        return !path.startsWith("/git/") && ROOT_GIT_PATH.matcher(path).matches();
    }

    private String buildRedirectTarget(String path, String query) {
        String target = "/git" + path;
        if (query == null || query.isBlank()) {
            return target;
        }
        return target + "?" + query;
    }
}
