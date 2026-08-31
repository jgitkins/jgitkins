package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * The git chain refuses everything, and no header can talk it out of that.
 *
 * <p>Task 2.127-A. What this replaced: the chain was {@code permitAll}, and git authorization took
 * its only identity from a client-supplied {@code X-User-Id} header that no reverse proxy in this
 * repository sets. {@code GitSmartHttpAuthFilter} checked that an {@code Authorization} header was
 * present without parsing it, because no {@code httpBasic()} was ever installed --
 * {@code addFilterBefore(..., BasicAuthenticationFilter.class)} names a position, it does not add
 * that filter. {@code PatAuthenticationProvider} and its BCrypt verification were implemented and
 * called by nobody.
 *
 * <p><strong>Why a fence rather than an authentication mechanism.</strong> Nothing serves these paths:
 * no {@code GitServlet}, no {@code ServletRegistrationBean}, and the two pack factories that would
 * drive one have zero consumers. So the risk was never "this is exploitable today" -- it was "wiring
 * the servlet turns on a git endpoint with no authentication, while every part named above makes it
 * look like there already is some". Installing httpBasic now would close that too, but it would also
 * settle how git authenticates -- PAT-over-Basic versus SSH keys, what {@code ROLE_GIT} means, how a
 * public repository's fetch stays anonymous -- against dead code, with no way to exercise the result
 * end to end. Those belong with the servlet work; task 2.127-B carries them.
 *
 * <p>{@code denyAll} is the version of "closed" that cannot be reopened by accident: a servlet
 * registered without deliberately editing that line answers 403 to every request, loudly, rather than
 * serving repositories to anyone who asks.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
        + "net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerTraceAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration,"
        + "net.devh.boot.grpc.server.autoconfigure.GrpcReflectionServiceAutoConfiguration"})
@AutoConfigureMockMvc
class GitChainDeniesByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} is refused")
    @ValueSource(strings = {
            "/git/alice/demo.git/info/refs",
            "/git/alice/demo.git/git-upload-pack",
            "/git/alice/demo.git/git-receive-pack"})
    void everyGitPathIsRefused(String uri) throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(uri)).andReturn().getResponse().getStatus();

        assertThat(status)
                .as("the git chain denies every request; 404 would mean the chain did not match and "
                        + "something else answered, which is a different and weaker guarantee")
                .isEqualTo(403);
    }

    @Test
    void aSpoofedRequesterHeaderChangesNothing() throws Exception {
        // The exact request that used to name a user. X-User-Id was the whole of git identity, so
        // this header decided who the caller was; now it is not read anywhere.
        int status = mockMvc.perform(MockMvcRequestBuilders.get("/git/alice/demo.git/info/refs")
                        .header("X-User-Id", "1"))
                .andReturn().getResponse().getStatus();

        assertThat(status).as("a client-supplied identity header must not change the outcome")
                .isEqualTo(403);
    }

    @Test
    void theNonCanonicalPathRedirectsIntoTheFencedChain() throws Exception {
        // GitSmartHttpCanonicalRedirectFilter is registered globally, not on this chain, so it still
        // runs and sends /{ns}/{repo}.git to /git/{ns}/{repo}.git. Asserted so that a later change to
        // that filter cannot quietly route git traffic around the fence.
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/alice/demo.git/info/refs")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(308);
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/git/alice/demo.git/info/refs");
    }
}
