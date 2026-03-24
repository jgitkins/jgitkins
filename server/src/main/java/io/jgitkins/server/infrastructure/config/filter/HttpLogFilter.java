package io.jgitkins.server.infrastructure.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpLogFilter extends OncePerRequestFilter {
    private static final int MAX_BODY_LOG_LENGTH = 20;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String method = requestWrapper.getMethod();
        String path = requestWrapper.getRequestURI();

        try {
            String requestBody = truncateBody(readBody(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding()));
            log.info("[SERVER] [REQUEST-RECEIVED] [METHOD {}] [PATH {}] [BODY {}]", method, path, requestBody);
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            String responseBody = truncateBody(readBody(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding()));
            log.info("[SERVER] [RESPONSE-SENT] [METHOD {}] [PATH {}] [BODY {}]", method, path, responseBody);
            responseWrapper.copyBodyToResponse();
        }
    }

    private String readBody(byte[] body, String encoding) {
        if (body == null || body.length == 0) {
            return "";
        }
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new String(body, charset);
    }

    private String truncateBody(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= MAX_BODY_LOG_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LOG_LENGTH);
    }
}
