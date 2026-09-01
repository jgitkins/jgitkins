package io.jgitkins.server.common.infrastructure.config.security.handler;

import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.server.common.presentation.error.PresentationProblemSpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Answers an unauthenticated request, and says so in the log.
 *
 * <p>Reached two ways: as the api chain's {@code authenticationEntryPoint}
 * ({@code SecurityConfig:142}) and directly from {@code JwtAuthenticationFilter:58} when a bearer
 * token is rejected. Logging here rather than at either call site covers both.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAnauthorizeHandler implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // WARN, not DEBUG. With the api chain closed by default, legitimate traffic carries a Bearer
        // token (app-web attaches one globally, ApiClientConfig:23-29) and anonymous browsing lands on
        // PublicApiRoutes, so a 401 here is an expired session, a prober, or a public route missing
        // from the list. The third is the reason this line exists and it must not be filtered out by
        // default. If a prober ever makes this noisy, the answer is a rate limit, not silence.
        log.warn("Refused unauthenticated request: {}", DeniedRequestLog.describe(request, authException));
        ApiResponse<Void> payload = ApiResponse.errorBody(
                PresentationProblemSpec.UNAUTHORIZED,
                PresentationProblemSpec.UNAUTHORIZED.getDefaultMessage(),
                "presentation");
        responseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, payload);
    }
}
