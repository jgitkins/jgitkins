package io.jgitkins.server.identity.access.adapter.in.support;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Converts a transport principal name into a numeric requester id, inside the identity inbound adapter.
 *
 * <p>Task 2.63. This is where current-user derivation for signup activation now lives. It used to happen
 * inside {@code UserProfileService} via {@code CurrentUserPort}, which put a transport concern in the
 * application layer and made the use case impossible to call with an explicit actor.
 *
 * <p><strong>Absent and malformed are different outcomes, deliberately.</strong> A missing principal is
 * an unauthenticated request and returns {@link Optional#empty()}, letting the caller decide the
 * response. A principal that is present but not a usable id is a broken credential, and it throws.
 * Collapsing the two into {@code empty()} would let a malformed subject take the same path as an
 * anonymous one — which, for an endpoint that mutates the caller's own account, means a corrupted token
 * would be reported as "please log in" and quietly retried rather than investigated.
 *
 * <p>{@code "0"} is malformed, not absent. No user has id zero, so a subject of {@code "0"} is either a
 * placeholder that leaked from somewhere or a truncated value; treating it as a valid id would attempt an
 * activation against a row that cannot exist, and treating it as absent would hide it.
 *
 * <p>Deliberately stricter than the collaboration context's resolver of the same simple name, which uses
 * {@code Long.valueOf} and swallows every failure into {@code empty()}. The two beans are distinct and
 * are not reused across contexts: this one is registered as
 * {@code identityRequesterUserIdResolver} and the collaboration one keeps the default name. Sharing a
 * single resolver would mean one context's error semantics silently governed the other's endpoints.
 */
@Component("identityRequesterUserIdResolver")
public class RequesterUserIdResolver {

    /**
     * @param principalName the transport principal name, or {@code null} when the request is anonymous
     * @return the requester id, or empty when no principal was presented
     * @throws ApplicationException when a principal was presented but is not a usable id
     */
    public Optional<Long> resolve(String principalName) {
        if (principalName == null || isBlank(principalName)) {
            return Optional.empty();
        }
        return Optional.of(parseOrReject(principalName));
    }

    /**
     * ASCII-only blank check.
     *
     * <p>{@link String#isBlank()} treats every Unicode whitespace character as blank, including ones
     * that could plausibly appear inside a mangled token. Those must reach the malformed branch and
     * throw, not be reported as an absent principal.
     */
    private static boolean isBlank(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != ' ' && character != '\t' && character != '\n' && character != '\r') {
                return false;
            }
        }
        return true;
    }

    private static long parseOrReject(String principalName) {
        for (int index = 0; index < principalName.length(); index++) {
            char character = principalName.charAt(index);
            // Explicitly ASCII: Character.isDigit accepts Devanagari and Arabic-Indic digits, which
            // Long.parseLong then also accepts, so a subject of "٥" would parse to 5. An id that depends
            // on the script it was written in is not an id.
            if (character < '0' || character > '9') {
                throw unauthenticated();
            }
        }
        long parsed;
        try {
            parsed = Long.parseLong(principalName);
        } catch (NumberFormatException overflow) {
            // All-digit but longer than a long: not a usable id.
            throw unauthenticated();
        }
        if (parsed <= 0) {
            throw unauthenticated();
        }
        return parsed;
    }

    private static ApplicationException unauthenticated() {
        // The message is intentionally the same as the anonymous case's. The distinction between absent
        // and malformed matters to this code and to the log, not to the caller: telling a client which
        // of the two it hit describes the server's view of its credential.
        return new ApplicationException(ApplicationProblemSpec.UNAUTHENTICATED, "Authentication required");
    }
}
