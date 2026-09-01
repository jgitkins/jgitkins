package io.jgitkins.server.common.infrastructure.config.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The api chain refuses by default as of {@code 8eb64b5}, and the first risk of that flip is a
 * legitimate public route answering 401 because it is missing from {@code PublicApiRoutes}.
 * {@code RouteAuthenticationContractTest} cannot see that: it checks the list that was declared, so a
 * wrong list is a self-consistent list. The log line is the only thing that surfaces it, which makes
 * the log line a behaviour and not a debugging aid.
 *
 * <p>These tests assert it exists, at a level that is not filtered by default, carrying the four
 * facts that make it actionable -- and that the two things deliberately kept OUT stay out. A refusal
 * log that echoes the credential it refused is worse than no log.
 */
class DeniedRequestLoggingTest {

    private ListAppender<ILoggingEvent> unauthorized;
    private ListAppender<ILoggingEvent> forbidden;
    private Logger unauthorizedLogger;
    private Logger forbiddenLogger;

    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(new ObjectMapper());
    private final ApiAnauthorizeHandler entryPoint = new ApiAnauthorizeHandler(writer);
    private final ApiAccessDeniedHandler accessDenied = new ApiAccessDeniedHandler(writer);

    @BeforeEach
    void attachAppenders() {
        unauthorizedLogger = (Logger) LoggerFactory.getLogger(ApiAnauthorizeHandler.class);
        forbiddenLogger = (Logger) LoggerFactory.getLogger(ApiAccessDeniedHandler.class);
        unauthorized = new ListAppender<>();
        forbidden = new ListAppender<>();
        unauthorized.start();
        forbidden.start();
        unauthorizedLogger.addAppender(unauthorized);
        forbiddenLogger.addAppender(forbidden);
    }

    @AfterEach
    void detachAppenders() {
        unauthorizedLogger.detachAppender(unauthorized);
        forbiddenLogger.detachAppender(forbidden);
        SecurityContextHolder.clearContext();
    }

    @Test
    void anAnonymousRefusalNamesTheRouteThatRefusedIt() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        entryPoint.commence(get("GET", "/api/organizes"), new MockHttpServletResponse(),
                new BadCredentialsException("bad"));

        ILoggingEvent event = only(unauthorized);
        assertThat(event.getLevel())
                .as("DEBUG or lower is filtered out by default, and a public route answering 401 would "
                        + "then be invisible in exactly the deployment that needs to see it")
                .isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .as("method and path are what identify a misclassified route; requester=anonymous is "
                        + "what distinguishes it from an expired session")
                .contains("GET")
                .contains("/api/organizes")
                .contains("requester=anonymous");
    }

    @Test
    void aRejectedBearerTokenIsLoggedRatherThanThrowing() throws Exception {
        // JwtAuthenticationFilter:58 calls commence(request, response, null). This is the invalid-token
        // path -- the most security-relevant 401 there is -- and a naive `authException.getClass()`
        // would turn it into a 500.
        entryPoint.commence(get("POST", "/api/repositories"), new MockHttpServletResponse(), null);

        assertThat(only(unauthorized).getFormattedMessage())
                .contains("POST")
                .contains("/api/repositories")
                .contains("reason=credential-rejected");
    }

    @Test
    void aForbiddenRequestNamesTheRequesterThatLackedTheAuthority() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(42L), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        accessDenied.handle(get("POST", "/api/runners"), new MockHttpServletResponse(),
                new AccessDeniedException("Access Denied"));

        ILoggingEvent event = only(forbidden);
        assertThat(event.getLevel())
                .as("the 403 line needs the same level guard as the 401 one -- it is the rarer and more "
                        + "informative of the two, so its disappearance would be the less noticed")
                .isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .as("a 403 means a real user was refused, and which user is the point of the line")
                .contains("POST")
                .contains("/api/runners")
                .contains("requester=42");
    }

    @Test
    void aPathCannotForgeASecondLogEntry() throws Exception {
        MockHttpServletRequest request = get("GET", "/api/x\r\nWARN  Refused nothing: forged entry");

        entryPoint.commence(request, new MockHttpServletResponse(), null);

        String message = only(unauthorized).getFormattedMessage();
        assertThat(message)
                .as("the path is caller input, so a newline in it must not become a new line in the log")
                .doesNotContain("\r")
                .doesNotContain("\n");
        assertThat(message)
                .as("the text may survive, flattened -- it is the line break that does the forging")
                .contains("/api/x..WARN");
    }

    @Test
    void theExceptionMessageNeverReachesTheLog() throws Exception {
        // The shape that makes this matter: an authentication exception raised while parsing a
        // credential, carrying that credential in its message.
        entryPoint.commence(get("GET", "/api/users"), new MockHttpServletResponse(),
                new BadCredentialsException("Failed to decode eyJhbGciOiJIUzI1NiJ9.SECRET.sig"));

        assertThat(only(unauthorized).getFormattedMessage())
                .as("only the exception class is logged, because the message can carry the credential")
                .doesNotContain("SECRET")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9")
                .contains("reason=BadCredentialsException");
    }

    @Test
    void theQueryStringNeverReachesTheLog() throws Exception {
        MockHttpServletRequest request = get("GET", "/api/users");
        request.setQueryString("access_token=SECRET");

        entryPoint.commence(request, new MockHttpServletResponse(), null);

        assertThat(only(unauthorized).getFormattedMessage())
                .as("callers put credentials in query strings, and this line is meant to be safe at WARN")
                .doesNotContain("SECRET");
    }

    @Test
    void anEmptySecurityContextReadsAsNoRequesterRatherThanBlankOrNull() throws Exception {
        SecurityContextHolder.clearContext();

        entryPoint.commence(get("GET", "/api/users"), new MockHttpServletResponse(), null);

        assertThat(only(unauthorized).getFormattedMessage())
                .as("no credential and no anonymous token is its own diagnosis, distinct from anonymous")
                .contains("requester=none");
    }

    @Test
    void aVeryLongPathIsTruncatedRatherThanFillingTheLine() throws Exception {
        entryPoint.commence(get("GET", "/api/" + "x".repeat(4000)), new MockHttpServletResponse(), null);

        String message = only(unauthorized).getFormattedMessage();
        assertThat(message)
                .as("the path is caller input, so one request must not be able to fill a log line")
                .contains("...")
                .hasSizeLessThan(400);
    }

    @Test
    void theResponseIsUnchangedByTheLogging() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(get("GET", "/api/users"), response, null);

        assertThat(response.getStatus())
                .as("logging is additive -- the body and status contract these handlers had is untouched")
                .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("error");
    }

    private static MockHttpServletRequest get(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    private static ILoggingEvent only(ListAppender<ILoggingEvent> appender) {
        assertThat(appender.list).as("exactly one line per refusal, or a log reader cannot count them")
                .hasSize(1);
        return appender.list.get(0);
    }
}
