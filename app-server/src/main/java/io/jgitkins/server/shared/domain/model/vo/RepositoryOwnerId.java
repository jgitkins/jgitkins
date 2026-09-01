package io.jgitkins.server.shared.domain.model.vo;

import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A repository's owner.
 *
 * <p><strong>Two factories, because the answer depends on where the value came from.</strong> The
 * rejected value is the same either way -- null or non-positive -- but who was wrong is not, and the
 * status code is a claim about that.
 *
 * <p>{@link #of} is for a value the client supplied. Rejecting it is a 400: the request was bad.
 * Seven call sites take this path, all of them resolving an owner out of a request.
 *
 * <p>{@link #fromStoredValue} is for a value read back out of the database. Its callers already skip
 * null -- a repository with no owner id is a legal aggregate -- so the only value that reaches the
 * check is a stored one that is zero or negative, which is a corrupt row. The request was fine, so
 * 400 would blame the wrong party and, worse, file server-side data corruption under client errors
 * where no alert is watching. That stays an unmapped exception and answers 500.
 *
 * <p>This narrows the rule {@code IdentifierInvariantTest} used to state, which converted only the
 * three identifiers whose application-layer guards tasks 2.95 and 2.96 removed. Those three are all
 * request-derived, so the old set is a subset of this one: same principle -- a value object broken by
 * internal code is a server bug and 500 is the honest answer -- applied by provenance rather than by
 * which guard happened to be deleted.
 */
@Getter
@EqualsAndHashCode
public class RepositoryOwnerId {
    private final Long value;

    private RepositoryOwnerId(Long value) {
        this.value = value;
    }

    /** For a value the client supplied. A bad one is a 400. */
    public static RepositoryOwnerId of(Long value) {
        if (value == null || value <= 0) {
            throw new InvalidIdentifierException("RepositoryOwnerId must be a positive value");
        }
        return new RepositoryOwnerId(value);
    }

    /**
     * For a value read out of storage. A bad one is a corrupt row, not a bad request, so it stays a
     * 500 -- and {@code storedRow} names the row so the 500 is actionable instead of anonymous.
     */
    public static RepositoryOwnerId fromStoredValue(Long value, String storedRow) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    "stored owner id is not positive: " + storedRow + " carries OWNER_ID=" + value);
        }
        return new RepositoryOwnerId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
