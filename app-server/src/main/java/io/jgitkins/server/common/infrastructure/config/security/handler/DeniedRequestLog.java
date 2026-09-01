package io.jgitkins.server.common.infrastructure.config.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * One home for what a refused request looks like in the log.
 *
 * <p>Until the api chain's default was flipped to {@code authenticated()} ({@code 8eb64b5}) nothing
 * logged a refusal at all: {@link ApiAnauthorizeHandler} and {@link ApiAccessDeniedHandler} wrote a
 * status and a body and returned. The flip's first risk is a legitimate public route answering 401
 * because it is missing from {@code PublicApiRoutes}, and {@code RouteAuthenticationContractTest}
 * cannot catch that -- it checks the list that was declared, so a wrong list is a consistent list.
 * Without a log line the way that reaches anyone is a user reporting it.
 *
 * <p>Both handlers log through here rather than each formatting its own line, so the two cannot drift
 * in what they record. It does not live in {@code SecurityErrorResponseWriter}, the shared path the
 * TODO offered as the alternative: that method takes {@code (response, status, payload)} and never
 * sees the request, so it cannot name the method or the path, which is the whole point of the line.
 *
 * <h2>What is deliberately not logged</h2>
 *
 * <p><strong>The exception message.</strong> Only the class name. An {@code AuthenticationException}
 * raised while parsing a credential can carry that credential in its message, and this line is meant
 * to be safe to keep at {@code WARN} on a public deployment. The class name answers "which kind of
 * refusal" without that risk.
 *
 * <p><strong>The query string.</strong> Same reason -- callers put tokens there.
 *
 * <p>The path IS logged, and it is caller input, so {@link #sanitize} strips anything that could
 * forge a second log entry. Tomcat rejects a raw CR or LF in the request line, so this is the second
 * line of defence rather than the first; it also covers the encoded and control-character cases and
 * any future caller that reaches these handlers without going through the connector.
 * {@code OidcIdTokenVerifierAdapter:61-63} left the provider name out of its log for this same class
 * of reason.
 */
final class DeniedRequestLog {

    /** Long enough for the deepest git route, short enough that one line cannot flood a log. */
    private static final int MAX_PATH = 256;

    private DeniedRequestLog() {
    }

    /**
     * {@code METHOD /path requester=<...> reason=<...>}, ready to hand to a single-argument log call.
     *
     * <p>One preformatted string rather than four placeholders because both call sites must produce
     * the same shape, and a parameterised call is where that shape would drift.
     */
    static String describe(HttpServletRequest request, Throwable reason) {
        return sanitize(request.getMethod()) + " " + sanitize(request.getRequestURI())
                + " requester=" + requester()
                + " reason=" + reasonOf(reason);
    }

    /**
     * Who the security context says is asking. Three answers, and the difference between them is the
     * diagnosis: {@code none} means the request carried no credential and no anonymous token either
     * (the JWT filter clears the context before delegating), {@code anonymous} means the chain saw an
     * unauthenticated caller and refused it anyway -- the misclassified-public-route case -- and an id
     * means a real user lacked an authority, which is the 403 case.
     */
    private static String requester() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "none";
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        // AuthenticatedUser#getName answers the numeric user id, which is an identifier and not a
        // credential. Sanitized anyway: a principal type added later need not be numeric.
        return sanitize(authentication.getName());
    }

    /**
     * {@code JwtAuthenticationFilter:58} calls {@code commence(request, response, null)} on a rejected
     * bearer token -- the most security-relevant 401 there is -- so null is a real input here, not a
     * defensive nicety.
     */
    private static String reasonOf(Throwable reason) {
        return reason == null ? "credential-rejected" : reason.getClass().getSimpleName();
    }

    /**
     * Keeps caller input to one line and to printable characters.
     *
     * <p>Anything outside printable ASCII becomes {@code .} -- that covers CR and LF, which is what
     * would let a caller forge a log entry, and it also removes the terminal escape sequences that
     * turn a log viewer into an attack surface. Non-ASCII paths lose their characters rather than
     * their length, which is a fair trade for a line nobody reads until something is wrong.
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "-";
        }
        int length = Math.min(value.length(), MAX_PATH);
        StringBuilder out = new StringBuilder(length + 3);
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            out.append(c >= 0x20 && c < 0x7f ? c : '.');
        }
        if (value.length() > MAX_PATH) {
            out.append("...");
        }
        return out.toString();
    }
}
