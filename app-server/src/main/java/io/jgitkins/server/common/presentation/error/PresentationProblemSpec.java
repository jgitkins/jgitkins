package io.jgitkins.server.common.presentation.error;

import io.jgitkins.core.common.problem.ProblemSpec;
import lombok.Getter;

@Getter
public enum PresentationProblemSpec implements ProblemSpec<PresentationErrorCode> {
    UNAUTHORIZED(PresentationErrorCode.UNAUTHORIZED, "REQ-401", "Unauthorized", "request.unauthorized"),
    NOT_FOUND(PresentationErrorCode.NOT_FOUND, "REQ-404", "Resource not found", "request.notFound"),
    METHOD_NOT_ALLOWED(PresentationErrorCode.METHOD_NOT_ALLOWED, "REQ-405", "Method not allowed", "request.methodNotAllowed"),
    UNSUPPORTED_MEDIA_TYPE(PresentationErrorCode.UNSUPPORTED_MEDIA_TYPE, "REQ-415", "Unsupported media type", "request.unsupportedMediaType"),
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

}
