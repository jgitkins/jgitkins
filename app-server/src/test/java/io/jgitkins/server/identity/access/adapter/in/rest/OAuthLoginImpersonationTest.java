package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Knowing a registered address must not be enough to log in as its owner.
 *
 * <p>Until task 2.114 it was. {@code POST /api/auth/oauth/login} is {@code permitAll} — it must be,
 * the caller has no token yet — and the request body carried {@code provider}, {@code subject} and
 * {@code email} as the identity of the person logging in. A request naming a known address was
 * answered with a JWT for that account: no credential, no OAuth handshake, no provider involved. The
 * route belongs to app-web, which had genuinely verified those claims before sending them, and
 * nothing distinguished app-web's request from anyone else's.
 *
 * <p>Runs through the real filter chain rather than a slice, because half of what this asserts is
 * that the route is reachable unauthenticated and is refused anyway. A slice with
 * {@code addFilters = false} would prove the controller rejects the body and say nothing about who
 * can reach it.
 *
 * <p>Every request here is answered 400 or 401. Neither is 200, which is what the same requests
 * returned before, and neither creates a row.
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
class OAuthLoginImpersonationTest {

    private static final String ROUTE = "/api/auth/oauth/login";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anIdentityAssertedInTheBodyIsNotAnIdentity() throws Exception {
        // The pre-2.114 request, verbatim. It was answered 200 with the account's JWT.
        MvcResult result = perform("""
                {"provider":"google","subject":"attacker-subject","email":"victim@corp.test",
                 "name":"Not The Victim","emailVerified":true,"avatarUrl":"https://img/x.png"}
                """);

        assertThat(result.getResponse().getStatus())
                .as("the body names an account and carries no token; there is nothing here to verify")
                .isEqualTo(400);
        assertThat(result.getResponse().getContentAsString())
                .as("and no token comes back")
                .doesNotContain("appToken");
    }

    @Test
    void aTokenThatIsNotATokenIsRefused() throws Exception {
        MvcResult result = perform("{\"provider\":\"google\",\"idToken\":\"not-a-jwt\"}");

        assertThat(result.getResponse().getStatus())
                .as("unparseable, so unverifiable")
                .isEqualTo(401);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("appToken");
    }

    @Test
    void anUnconfiguredProviderIsRefusedTheSameWayABadTokenIs() throws Exception {
        MvcResult result = perform("{\"provider\":\"not-configured\",\"idToken\":\"not-a-jwt\"}");

        assertThat(result.getResponse().getStatus())
                .as("same answer as a bad token, so the response cannot be used to enumerate providers")
                .isEqualTo(401);
    }

    @Test
    void aRefusedLoginNeverReachesPersistence() throws Exception {
        // The other half of the old failure: signup saved the USER row and validated the identity
        // afterwards, with no transaction around either, so a rejected request left a permanent
        // account whose username UsernameAllocator would never hand out again.
        //
        // This context runs on an empty H2 with no schema, which makes the assertion sharper than a
        // row count would be: anything that gets as far as UserService fails on a missing table and
        // answers 500. A 401 is proof the request was refused before the first write, and if
        // verification were removed this test would report that 500.
        MvcResult result = perform("""
                {"provider":"google","subject":"attacker-subject","email":"ghost@corp.test",
                 "idToken":"not-a-jwt"}
                """);

        assertThat(result.getResponse().getStatus())
                .as("refused at verification, before any persistence was attempted")
                .isEqualTo(401);
    }

    private MvcResult perform(String body) throws Exception {
        return mockMvc.perform(post(ROUTE).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }
}
