package io.jgitkins.server.support;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import io.jgitkins.server.shared.application.security.AuthenticatedUser;

/**
 * Puts an authenticated subject into the security context for controller tests.
 *
 * <p>Controllers read the caller with {@code @CurrentUser AuthenticatedUser}, which resolves from
 * {@link SecurityContextHolder} — not from the servlet request. That is why
 * {@code MockMvc.principal(...)} has no effect on these routes: it sets the request principal, the
 * resolver never looks there, the parameter arrives null, and the response is a 401 that looks like an
 * authorization bug rather than a missing test fixture. This helper exists so that trap is written down
 * once instead of being rediscovered per test class.
 *
 * <p>The principal is an {@code AuthenticatedUser}, matching what {@code JwtAuthenticationFilter}
 * sets. It implements {@code Principal}, so {@code authentication.getName()} still answers the
 * numeric id for {@code CurrentUserSecurityAdapter} and the other two consumers that parse it —
 * setting a bare record here would make those pass in tests and fail in production.
 *
 * <p>Always pair {@link #authenticateAs} with {@link #clear} in an {@code @AfterEach}. The context is a
 * thread local, and a leftover authentication leaks an authenticated caller into whichever test class
 * runs next on the same thread — which surfaces as a test that passes alone and fails in the suite.
 */
public final class TestAuthentication {

    private TestAuthentication() {
    }

    public static void authenticateAs(String subject) {
        authenticateAs(Long.valueOf(subject));
    }

    /**
     * The typed form, which is what production sets.
     *
     * <p>The string overload is kept so the ~30 existing call sites did not all have to change in the
     * same commit as the principal type; it parses and delegates here.
     */
    public static void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId), "", List.of()));
    }

    /** Leaves the context genuinely anonymous, for the tests that assert a 401. */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
