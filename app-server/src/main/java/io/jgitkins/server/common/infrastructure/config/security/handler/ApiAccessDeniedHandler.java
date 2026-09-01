package io.jgitkins.server.common.infrastructure.config.security.handler;

import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Answers an authenticated request that lacked an authority, and says so in the log.
 *
 * <p>Today the only rules that can produce this are the two {@code hasRole("RUNNER_ADMIN")} entries
 * at {@code SecurityConfig:115-116}, and nothing issues that authority yet, so every 403 from here is
 * currently a real user being turned away from runner registration. That is worth a line on its own.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        // A 403 is rarer and more informative than a 401: the caller proved who they are and the chain
        // still refused, so this is an authority the deployment has not granted to anyone.
        log.warn("Refused authorized request: {}", DeniedRequestLog.describe(request, accessDeniedException));
        ApiResponse<Void> payload = ApiResponse.errorBody(
                ApplicationProblemSpec.ACCESS_DENIED,
                ApplicationProblemSpec.ACCESS_DENIED.getDefaultMessage(),
                "application");
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, payload);
    }
}
