package io.jgitkins.server.common.presentation.error;

import io.jgitkins.core.common.problem.ProblemSpec;

public enum PresentationProblemSpec implements ProblemSpec<PresentationErrorCode> {
    UNAUTHORIZED(PresentationErrorCode.UNAUTHORIZED, "REQ-401", "Unauthorized", "request.unauthorized"),
    BAD_REQUEST(PresentationErrorCode.BAD_REQUEST, "REQ-400", "Bad request", "request.bad");

    private final PresentationErrorCode errorCode;
    private final String code;
    private final String defaultMessage;
    private final String messageKey;

    PresentationProblemSpec(PresentationErrorCode errorCode, String code, String defaultMessage, String messageKey) {
        this.errorCode = errorCode;
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.messageKey = messageKey;
    }

    @Override
    public PresentationErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
