package io.jgitkins.server.support;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Puts an authenticated subject into the security context for controller tests.
 *
 * <p>Controllers read the caller with {@code @AuthenticationPrincipal(expression = "username")}, which
 * resolves from {@link SecurityContextHolder} — not from the servlet request. That is why
 * {@code MockMvc.principal(...)} has no effect on these routes: it sets the request principal, the
 * resolver never looks there, the parameter arrives null, and the response is a 401 that looks like an
 * authorization bug rather than a missing test fixture. This helper exists so that trap is written down
 * once instead of being rediscovered per test class.
 *
 * <p>The subject is the numeric user id as a string, because that is what {@code authentication.getName()}
 * carries at runtime — see {@code CurrentUserSecurityAdapter}, which parses it with {@code Long.valueOf}.
 *
 * <p>Always pair {@link #authenticateAs} with {@link #clear} in an {@code @AfterEach}. The context is a
 * thread local, and a leftover authentication leaks an authenticated caller into whichever test class
 * runs next on the same thread — which surfaces as a test that passes alone and fails in the suite.
 */
public final class TestAuthentication {

    private TestAuthentication() {
    }

    public static void authenticateAs(String subject) {
        User principal = new User(subject, "", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", List.of()));
    }

    /** Leaves the context genuinely anonymous, for the tests that assert a 401. */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
