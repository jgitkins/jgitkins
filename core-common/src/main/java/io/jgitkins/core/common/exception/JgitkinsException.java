package io.jgitkins.core.common.exception;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.problem.ProblemSpec;

public abstract class JgitkinsException extends RuntimeException {
    private final ErrorCode errorCode;
    private final ProblemSpec<? extends ErrorCode> problemSpec;

    public JgitkinsException(ErrorCode errorCode) {
        this(errorCode, null, null, null);
    }

    public JgitkinsException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public JgitkinsException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }

    public JgitkinsException(ProblemSpec<? extends ErrorCode> problemSpec) {
        this(problemSpec, null, null);
    }

    public JgitkinsException(ProblemSpec<? extends ErrorCode> problemSpec, String message) {
        this(problemSpec, message, null);
    }

    public JgitkinsException(ProblemSpec<? extends ErrorCode> problemSpec, String message, Throwable cause) {
        this(problemSpec.getErrorCode(), message, cause, problemSpec);
    }

    private JgitkinsException(ErrorCode errorCode, String message, Throwable cause,
            ProblemSpec<? extends ErrorCode> problemSpec) {
        super((message == null || message.isBlank())
                ? resolveDefaultMessage(errorCode, problemSpec)
                : message, cause);
        this.errorCode = errorCode;
        this.problemSpec = problemSpec;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getProblemCode() {
        if (problemSpec != null) {
            return problemSpec.getCode();
        }
        return errorCode.getCode();
    }

    public String getDefaultMessage() {
        return resolveDefaultMessage(errorCode, problemSpec);
    }

    public String getMessageKey() {
        return problemSpec == null ? null : problemSpec.getMessageKey();
    }

    public ProblemSpec<? extends ErrorCode> getProblemSpec() {
        return problemSpec;
    }

    private static String resolveDefaultMessage(ErrorCode errorCode, ProblemSpec<? extends ErrorCode> problemSpec) {
        if (problemSpec != null) {
            return problemSpec.getDefaultMessage();
        }
        return errorCode.getDefaultMessage();
    }
}
