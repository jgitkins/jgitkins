package io.jgitkins.server.common.presentation.advice.mapper;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.core.common.error.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ApplicationErrorHttpStatusMapper implements ErrorHttpStatusMapper {

    @Override
    public boolean supports(ErrorCode errorCode) {
        return errorCode instanceof ApplicationErrorCode;
    }

    @Override
    public HttpStatus map(ErrorCode errorCode) {
        return mapApplication((ApplicationErrorCode) errorCode);
    }

    private HttpStatus mapApplication(ApplicationErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

}
