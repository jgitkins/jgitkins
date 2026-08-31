package io.jgitkins.server.shared.application.security;

import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import java.security.Principal;

/**
 * The requester, as a type.
 *
 * <p>Replaces {@code @AuthenticationPrincipal(expression = "username") String subject} plus a
 * per-context resolver that turned the string back into a number. That round trip cost two
 * production 500s: {@code 93294fa} and {@code d74813d}. Both had the same shape — the expression is
 * evaluated against whatever object happens to be the principal, so an anonymous request (principal
 * is the String {@code "anonymousUser"}) or an OAuth2 session (principal is a {@code DefaultOidcUser})
 * blew up with {@code SpelEvaluationException EL1008E} on every route that read the requester. A
 * type cannot be asked for a property it does not have.
 *
 * <h2>Why this lives in shared</h2>
 *
 * <p>Controllers in five bounded contexts need it. {@code ChangeReviewBoundedContextArchitectureTest}
 * forbids {@code io.jgitkins.server.identity} inside {@code /change/review/} outside the ACL
 * allowlist, so the natural-looking home in {@code identity.access} would have blocked
 * {@code MergeController} — a wall this codebase already hit once, which is why
 * {@code ReviewRequesterResolver} exists. Every context already imports
 * {@code io.jgitkins.server.shared}.
 *
 * <h2>Why it implements Principal</h2>
 *
 * <p>{@code AbstractAuthenticationToken.getName()} falls back to {@code getPrincipal().toString()}
 * for a principal it does not recognise. A bare record would therefore make {@code getName()} return
 * {@code "AuthenticatedUser[userId=7]"}, and three live consumers parse that value as a number:
 * {@code CurrentUserSecurityAdapter} (which every personal-access-token route reaches through
 * {@code ActiveAccountPolicyPort}), {@code PushEventRequestAdapter}, and
 * {@code PatAuthenticationProvider}. All three would have started failing closed — every PAT route
 * answering 401 to its rightful owner — for a reason no test in those files would have named.
 *
 * <p>{@code java.security.Principal} rather than Spring Security's {@code AuthenticatedPrincipal}:
 * both satisfy that lookup, and the JDK interface keeps this type free of a framework dependency it
 * has no other use for.
 *
 * <h2>No roles</h2>
 *
 * <p>Deliberately absent. Nothing in the codebase reads authorities to make a decision — the
 * authority list exists, is populated, and has zero consumers. Carrying roles here would imply an
 * authorization model that is not wired, and the first reader to trust that implication would write
 * a check that never fires. Task 2.89 decides whether roles get connected or removed.
 */
public record AuthenticatedUser(Long userId) implements Principal {

    public AuthenticatedUser {
        if (userId == null || userId <= 0) {
            // The token codec already rejects a non-positive subject. This is the second line, and it
            // is here so that a future caller constructing one by hand cannot skip the rule.
            throw new IllegalArgumentException("userId must be a positive value");
        }
    }

    /**
     * The id, or null when nobody is logged in.
     *
     * <p>Exists so a read route can say what it means in one place. Null is not a failure on a route
     * that serves public repositories: {@code canRead} checks PUBLIC before membership, so an
     * anonymous caller is a legitimate reader and the visibility rule decides. Routes that require a
     * requester reject the null themselves and answer 401 — the asymmetry between the two is
     * deliberate and is asserted per route.
     */
    public static Long userIdOrNull(AuthenticatedUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }

    /**
     * The id, or 401 when nobody is logged in.
     *
     * <p>The counterpart to {@link #userIdOrNull}, and it lives here for the same reason. Nine
     * controllers held a byte-identical private copy of this method while the permissive half was
     * centralised -- so the branch that decides whether a write is refused was the copy-pasted one.
     * That asymmetry is backwards: a drifting copy of {@code userIdOrNull} loosens nothing, and a
     * drifting copy of this changes what an unauthenticated caller is told.
     *
     * <p>{@code OrganizeController} keeps its own, with a different message, on purpose -- see the
     * comment there. It is the only exception, and it is one because a wire contract already shipped.
     */
    public static Long requireUserId(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            throw new UnauthenticatedException("Authentication required");
        }
        return currentUser.userId();
    }

    /**
     * The numeric id as a string.
     *
     * <p>Not a display name. The three consumers named above call {@code Long.valueOf} on it, so
     * returning anything else — a username, a formatted label — would break them silently.
     */
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
