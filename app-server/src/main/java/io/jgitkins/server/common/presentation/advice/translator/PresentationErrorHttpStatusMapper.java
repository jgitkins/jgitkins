package io.jgitkins.server.common.presentation.advice.translator;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.server.common.presentation.error.PresentationErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class PresentationErrorHttpStatusMapper implements ErrorHttpStatusMapper {

    @Override
    public boolean supports(ErrorCode errorCode) {
        return errorCode instanceof PresentationErrorCode;
    }

    @Override
    public HttpStatus map(ErrorCode errorCode) {
        PresentationErrorCode presentationErrorCode = (PresentationErrorCode) errorCode;
        return switch (presentationErrorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
        };
    }
}
