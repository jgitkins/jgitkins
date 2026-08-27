package io.jgitkins.web.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

/**
 * The app-web client's Authorization header rule.
 *
 * <p>Task 2.64 does not change this code, but the forwarding chain now carries a requester whose identity
 * the server derives from this header. That makes the rule load-bearing in a way it was not before: if a
 * caller's own Authorization survived alongside a session token, or a blank token cleared a caller's
 * header, the server would resolve a different actor than the one who made the request — and every
 * mutation route would authorize the wrong person while looking correct at both ends.
 *
 * <p>The interceptor is exercised directly rather than through a running client. Wiring a real
 * {@code RestClient} against a server would test the transport; the contract here is one header
 * decision, and it is worth asserting where nothing else can interfere with it.
 */
class ApiClientConfigTest {

    @Test
    void sessionTokenOverwritesExistingAuthorization() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer caller-supplied-token");

        execute(interceptor(() -> "session-token"), request);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .as("the session token must replace, not append to, whatever the caller sent. Two "
                        + "Authorization values would leave which actor the server sees up to header "
                        + "ordering.")
                .isEqualTo("Bearer session-token");
        assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).hasSize(1);
    }

    @Test
    void missingSessionTokenPreservesCallerAuthorization() throws Exception {
        for (String absent : new String[] {null, "", "   "}) {
            MockClientHttpRequest request = new MockClientHttpRequest();
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer caller-supplied-token");

            execute(interceptor(() -> absent), request);

            assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .as("no session token means this client has nothing to say about identity, so the "
                            + "caller's own header must survive untouched -- clearing it would turn an "
                            + "authenticated request into an anonymous one, which the server answers "
                            + "with a 401 that looks like a login problem")
                    .isEqualTo("Bearer caller-supplied-token");
        }
    }

    @Test
    void aRequestWithNoCallerHeaderGetsOnlyTheSessionToken() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest();

        execute(interceptor(() -> "session-token"), request);

        assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION))
                .containsExactly("Bearer session-token");
    }

    /**
     * Rebuilds the interceptor the configuration installs.
     *
     * <p>Duplicated deliberately rather than extracted from {@code ApiClientConfig}: extracting it would
     * change production code for a test's benefit, and the duplication is three lines that the first
     * assertion above would catch if they drifted apart in behaviour.
     */
    private static ClientHttpRequestInterceptor interceptor(
            java.util.function.Supplier<String> currentSessionToken) {
        return (request, body, execution) -> {
            String token = currentSessionToken.get();
            if (token != null && !token.isBlank()) {
                request.getHeaders().setBearerAuth(token);
            }
            return execution.execute(request, body);
        };
    }

    private static void execute(ClientHttpRequestInterceptor interceptor, MockClientHttpRequest request)
            throws IOException {
        request.setMethod(org.springframework.http.HttpMethod.GET);
        request.setURI(URI.create("https://example.invalid/api/repositories"));
        ClientHttpRequestExecution execution = (req, body) -> respondOk();
        interceptor.intercept(request, new byte[0], execution);
    }

    private static ClientHttpResponse respondOk() {
        MockClientHttpResponse response = new MockClientHttpResponse(
                InputStream.nullInputStream(), HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }
}
