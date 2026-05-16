package io.jgitkins.server.infrastructure.config.security.handler;

import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.presentation.common.error.PresentationProblemSpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@RequiredArgsConstructor
public class ApiAnauthorizeHandler implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ApiResponse<Void> payload = ApiResponse.errorBody(
                PresentationProblemSpec.UNAUTHORIZED,
                PresentationProblemSpec.UNAUTHORIZED.getDefaultMessage(),
                "presentation");
        responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, payload);
    }
}
