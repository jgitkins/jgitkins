package io.jgitkins.server.change.review.adapter.in.support;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Turns the transport principal into a requester id for this context's inbound adapters.
 *
 * <p>Named distinctly rather than {@code RequesterUserIdResolver}, which already exists twice
 * (identity.access and collaboration) and is one of the collisions task 2.132 tracks. A third one
 * would make that worse.
 *
 * <p>Local rather than borrowed: change/review may not import from another bounded context outside
 * its ACL adapters, and {@code ChangeReviewBoundedContextArchitectureTest} enforces that. Reaching
 * into identity.access for this was the first thing tried and the guard rejected it, correctly --
 * a controller pulling another context's class is the coupling the rule exists to stop.
 *
 * <p>Strict, matching identity.access rather than collaboration. A principal that was presented but
 * cannot be parsed is rejected, not quietly demoted to anonymous: a mangled credential on a write
 * path should fail loudly. The two existing resolvers disagree on exactly this, which is the real
 * argument for consolidating all three into a shared inbound support class. That is a wider change
 * than this task and belongs with 2.132.
 */
@Component
public class ReviewRequesterResolver {

    /**
     * @param principalName the transport principal, or {@code null} when the request is anonymous
     * @return the requester id, or empty when no principal was presented at all
     * @throws ApplicationException when a principal was presented but is not a usable id
     */
    public Optional<Long> resolve(String principalName) {
        if (principalName == null || isAsciiBlank(principalName)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(principalName.trim()));
        } catch (NumberFormatException notAnId) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED,
                    "Authentication required");
        }
    }

    /**
     * ASCII-only, deliberately. {@link String#isBlank()} counts every Unicode whitespace character,
     * including ones that could plausibly appear inside a mangled token; those must reach the
     * malformed branch and throw rather than be reported as an absent principal.
     */
    private static boolean isAsciiBlank(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }
}
