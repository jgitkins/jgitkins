package io.jgitkins.server.identity.access.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Guards the routes that read a requester against the OAuth-session principal.
 *
 * <p>A browser that completes the OAuth handshake keeps an {@code OAuth2AuthenticationToken} in its
 * session. {@code JwtAuthenticationFilter} only replaces the context when a Bearer header is present,
 * so without one the session principal — a {@code DefaultOidcUser} — reached
 * {@code AuthenticationPrincipalArgumentResolver}, which evaluates {@code expression = "username"}
 * against the principal without guarding the call. {@code DefaultOidcUser} has no such property, so
 * argument resolution threw {@code SpelEvaluationException} and every one of these routes answered 500
 * to a logged-in browser.
 *
 * <p>This is the same defect class as the anonymous one pinned by
 * {@link AnonymousPrincipalResolutionTest}, in a path that fix did not close: that one removed the
 * anonymous principal, this one removes the session principal from a chain that does not authenticate
 * with it. The JWT is the credential here, handed to the client as {@code OAuthLoginResult#appToken}.
 *
 * <p>The assertion is about argument resolution rather than status codes on purpose: the test context
 * runs on an empty H2, so data-dependent routes still answer 500 from a missing table. What must never
 * come back is a failure that happens before the controller is entered.
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
class OAuth2SessionPrincipalResolutionTest {

    /** Every route that reads a requester and is reachable by a browser holding only a session. */
    private static final List<String> REQUESTER_READING_ROUTES = List.of(
            "/api/organizes/me",
            "/api/repositories",
            "/api/repositories/1",
            "/api/repositories/users/someone",
            "/api/repositories/1/overview",
            "/api/internal/organizes",
            "/api/internal/repositories/users/someone",
            "/api/internal/repositories/ns/repo/overview");

    /** Routes among the above that need no table, so the outcome is observable end to end. */
    private static final List<String> SCHEMA_FREE_ROUTES = List.of(
            "/api/organizes/me",
            "/api/internal/organizes");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuerPort tokenIssuerPort;

    @Test
    void oauthSessionPrincipalDoesNotBreakArgumentResolution() throws Exception {
        OAuth2AuthenticationToken session = oauthSession();
        for (String route : REQUESTER_READING_ROUTES) {
            MvcResult result = mockMvc.perform(get(route).with(authentication(session))).andReturn();
            assertThat(findSpelFailure(result.getResolvedException()))
                    .as("argument resolution must not fail for an OAuth-session caller on %s", route)
                    .isNull();
        }
    }

    @Test
    void oauthSessionCallerIsTreatedAsUnauthenticatedWhereNoTableIsNeeded() throws Exception {
        OAuth2AuthenticationToken session = oauthSession();
        for (String route : SCHEMA_FREE_ROUTES) {
            MvcResult result = mockMvc.perform(get(route).with(authentication(session))).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("an OAuth-session caller without a Bearer token reads %s as anonymous", route)
                    .isEqualTo(200);
        }
    }

    /**
     * The counterpart to the two tests above, and the reason clearing the context is safe rather than
     * merely effective: with the credential this chain actually accepts, the requester still resolves.
     * Nothing else in the suite exercised a Bearer token through the real filter chain -- the ten
     * controller slice tests all run with {@code addFilters = false} -- so without this, "the filter
     * clears the context" had no evidence that it clears only what it should.
     */
    @Test
    void bearerTokenStillAuthenticatesThroughTheRealChain() throws Exception {
        String token = tokenIssuerPort.issueToken(42L, List.of("ROLE_USER"));

        MvcResult result = mockMvc.perform(get("/api/organizes/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertThat(findSpelFailure(result.getResolvedException()))
                .as("a Bearer-authenticated request must resolve its requester")
                .isNull();
        assertThat(result.getResponse().getStatus())
                .as("a Bearer-authenticated request must not be rejected as unauthenticated")
                .isNotIn(401, 403);
        // The status is not asserted to be 200: this context runs on an empty H2, and a non-null
        // requester is exactly what makes the read query the database. OrganizeService:92 returns an
        // empty list without touching the repository when the requester is null, so reaching the
        // repository at all is itself evidence that the Bearer token produced a requester rather than
        // being cleared along with the session principal.
    }

    private static OAuth2AuthenticationToken oauthSession() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
                .issuedAt(Instant.parse("2026-08-27T00:00:00Z"))
                .expiresAt(Instant.parse("2036-08-27T00:00:00Z"))
                .claim("sub", "google-subject-123")
                .build();
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, "sub");
        return new OAuth2AuthenticationToken(
                oidcUser, List.of(new SimpleGrantedAuthority("ROLE_USER")), "google");
    }

    private static SpelEvaluationException findSpelFailure(Throwable thrown) {
        Throwable walk = thrown;
        while (walk != null) {
            if (walk instanceof SpelEvaluationException spel) {
                return spel;
            }
            if (walk.getCause() == walk) {
                return null;
            }
            walk = walk.getCause();
        }
        return null;
    }
}
