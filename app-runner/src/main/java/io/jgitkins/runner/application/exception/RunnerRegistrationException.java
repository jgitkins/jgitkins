package io.jgitkins.runner.application.exception;

/**
 * Signals that runner activation/registration with the server failed.
 */
public class RunnerRegistrationException extends RuntimeException {

    private final int statusCode;

    public RunnerRegistrationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public RunnerRegistrationException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
