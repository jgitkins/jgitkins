package io.jgitkins.server.infrastructure.config.security.handler;

import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        ApiResponse<Void> payload = ApiResponse.errorBody(
                ApplicationProblemSpec.ACCESS_DENIED,
                ApplicationProblemSpec.ACCESS_DENIED.getDefaultMessage(),
                "application");
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, payload);
    }
}
