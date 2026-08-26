package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;

/**
 * Thrown while the application context is being built, when a persistence selector property holds a
 * value that is not a known implementation.
 *
 * <p>This is deliberately fatal rather than a warning plus a default. A misspelled selector means
 * the operator's intent is unknown, and guessing it on a persistence cutover risks writing through
 * the wrong store for the lifetime of the deployment. Failing at startup surfaces the typo while it
 * is still cheap.
 *
 * <p>Carries {@code INTERNAL_ERROR} rather than a dedicated code because a startup failure never
 * reaches an HTTP error envelope: the application does not finish booting, so no request can
 * observe it.
 */
public class InvalidPersistenceSelectorException extends InfrastructureException {

    public InvalidPersistenceSelectorException(String propertyName, String rawValue, String allowedValues) {
        super(InfrastructureErrorCode.INTERNAL_ERROR,
                "Invalid persistence selector: " + propertyName + " was '" + rawValue
                        + "', allowed values are [" + allowedValues + "]");
    }
}
