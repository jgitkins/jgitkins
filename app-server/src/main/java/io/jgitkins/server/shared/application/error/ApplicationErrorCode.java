package io.jgitkins.server.shared.application.error;

import io.jgitkins.core.common.error.ErrorCode;
import lombok.Getter;

@Getter
public enum ApplicationErrorCode implements ErrorCode {
    UNAUTHENTICATED("UNAUTHENTICATED", "Authentication required"),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Requested resource not found"),
    ALREADY_EXISTS("ALREADY_EXISTS", "Resource already exists"),
    /** The resource exists and its current state forbids the request. Not a duplicate. */
    CONFLICT("CONFLICT", "Request conflicts with the current state"),
    UNPROCESSABLE("UNPROCESSABLE", "Request could not be processed"),
    /**
     * We could not reach a system we depend on. Distinct from every code above, which say the
     * request was wrong: this one says the request may well have been fine and the failure is ours.
     * Collapsing it into UNAUTHENTICATED would report an identity-provider outage to the user as
     * "your credentials are invalid" and hide the outage from whoever is watching 5xx.
     */
    UPSTREAM_UNAVAILABLE("UPSTREAM_UNAVAILABLE", "An upstream system is unavailable");

    private final String code;
    private final String defaultMessage;

    ApplicationErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

}
