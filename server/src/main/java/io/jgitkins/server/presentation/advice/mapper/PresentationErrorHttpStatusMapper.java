package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.presentation.common.error.PresentationErrorCode;
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
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
        };
    }
}
