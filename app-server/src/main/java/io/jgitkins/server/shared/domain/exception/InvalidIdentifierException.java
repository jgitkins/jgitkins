package io.jgitkins.server.shared.domain.exception;

import io.jgitkins.server.shared.domain.error.DomainProblemSpec;

/**
 * An identifier value object was handed something that is not a usable identifier.
 *
 * <p>Replaces the bare {@code IllegalArgumentException} these value objects used to throw. That one has
 * no handler, so it fell to {@code GlobalExceptionHandler}'s {@code Exception} catch-all and answered
 * 500: the server reporting its own failure for a value the caller supplied. Spring declines to map
 * {@code IllegalArgumentException} to 400 on purpose, because library code throws it for programming
 * errors, so the fix is a typed exception rather than a global mapping.
 *
 * <p>{@code RULE_VIOLATION} rather than {@code POLICY_VIOLATION}, matching how this codebase already
 * uses the two: {@code ORGANIZE_MEMBER_INVALID} is a rule violation for an invalid payload, while the
 * policy violations are the runner token cases. "Not a positive number" is a value rule.
 *
 * <p>Only the three identifiers whose application-layer guards tasks 2.95 and 2.96 remove were
 * converted. The rest keep throwing {@code IllegalArgumentException}, and keep answering 500, because a
 * value object broken by internal code is a server bug and 500 is the honest answer.
 */
public class InvalidIdentifierException extends DomainException {

    public InvalidIdentifierException(String message) {
        super(DomainProblemSpec.IDENTIFIER_INVALID, message);
    }
}
