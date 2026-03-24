package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.domain.error.DomainErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DomainErrorHttpStatusMapper implements ErrorHttpStatusMapper {

    @Override
    public boolean supports(ErrorCode errorCode) {
        return errorCode instanceof DomainErrorCode;
    }

    @Override
    public HttpStatus map(ErrorCode errorCode) {
        DomainErrorCode domainErrorCode = (DomainErrorCode) errorCode;
        return switch (domainErrorCode) {
            case ORGANIZE_MEMBER_INVALID,
                 RULE_VIOLATION -> HttpStatus.BAD_REQUEST;
            case ORGANIZE_MEMBER_ALREADY_EXISTS,
                 RUNNER_ALREADY_ACTIVED -> HttpStatus.CONFLICT;
            case RUNNER_TOKEN_INVALID,
                 USER_ALREADY_ACTIVATED -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
