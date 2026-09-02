package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Who may register, list and delete a runner.
 *
 * <p>What this closes: {@code RunnerManagementController} had no requester at all -- not {@code @CurrentUser},
 * not a read off the request. All five of its routes answered anyone. The worst of them is
 * {@code POST /api/runners}, which returns {@code RunnerRegistrationResult.token}: an anonymous
 * caller registered a runner, received an authentication token, presented it to
 * {@code POST /api/runners/activate}, and the activated runner then received dispatched jobs. That
 * was the one live P0 in this batch, and flipping the chain default is what closes it.
 *
 * <p>Authentication alone is not the whole answer for the two writes. Handing out a token and taking
 * an operational asset away are not things any logged-in user should do, so they require
 * {@code ROLE_RUNNER_ADMIN}. Nothing issues that authority yet -- {@code OAuthLoginService} issues
 * {@code ROLE_USER} and nothing else -- so both routes are closed to everyone until task 2.89
 * decides who holds it. Closed is the correct interim state: app-web calls no runner route, and
 * app-runner calls only {@code /activate}.
 *
 * <p>The read routes take {@code authenticated()}. They no longer leak the token
 * ({@code RunnerDetailResult} dropped it), so the exposure is runner metadata, and requiring a login
 * is proportionate to that.
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RunnerRouteAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuerPort tokenIssuerPort;

    @Test
    void anAnonymousCallerCannotRegisterARunnerAndCollectItsToken() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/runners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"anything\",\"scopeType\":\"GLOBAL\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("POST /api/runners answers with a runner authentication token. An anonymous "
                        + "caller reaching it is the live exposure this batch exists to close, and a "
                        + "400 here would mean the request was validated rather than refused")
                .isEqualTo(401);
    }

    @Test
    void anAnonymousCallerCannotDeleteARunner() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.delete("/api/runners/1"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void aLoggedInUserWithoutTheRunnerRoleCannotRegisterARunner() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/runners")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"anything\",\"scopeType\":\"GLOBAL\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("authenticated is not enough for a route that issues a credential. 403, not 401: "
                        + "the caller is known and still refused")
                .isEqualTo(403);
    }

    @Test
    void aLoggedInUserWithoutTheRunnerRoleCannotDeleteARunner() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.delete("/api/runners/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(403);
    }

    @Test
    void theRunnerRoleGetsPastTheChain() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.delete("/api/runners/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andReturn().getResponse().getStatus();

        // Not asserting the eventual status: this context runs on a schema-less H2, so the handler
        // fails on a missing table. Not-403 is the whole claim -- the rule admits the right authority
        // rather than refusing everyone, which is what makes the two 403s above meaningful instead of
        // a route that is simply switched off.
        assertThat(status)
                .as("ROLE_RUNNER_ADMIN must pass the chain, or the 403s above prove nothing")
                .isNotIn(401, 403);
    }

    @Test
    void anAnonymousRunnerCanStillActivate() throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.post("/api/runners/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-real-token\"}"))
                .andReturn().getResponse().getStatus();

        // app-runner's only call to app-server. It authenticates with the token in the body and can
        // never hold a JWT, so the chain must let it through and the handler must decide.
        assertThat(status)
                .as("a runner coming online has no JWT. Refusing this route at the chain locks every "
                        + "runner out permanently")
                .isNotIn(401, 403);
    }

    private String userToken() {
        return tokenIssuerPort.issueToken(1L, List.of("ROLE_USER"));
    }

    private String adminToken() {
        return tokenIssuerPort.issueToken(1L, List.of("ROLE_RUNNER_ADMIN"));
    }
}
