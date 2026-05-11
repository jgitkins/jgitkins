package io.jgitkins.server.repository.application.port.out.exception;

public class GitPortException extends RuntimeException {

    public GitPortException(String message) {
        super(message);
    }

    public GitPortException(String message, Throwable cause) {
        super(message, cause);
    }
}
