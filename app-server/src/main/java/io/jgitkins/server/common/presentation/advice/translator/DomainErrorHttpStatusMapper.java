package io.jgitkins.server.common.presentation.advice.translator;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.server.shared.domain.error.DomainErrorCode;
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
            case RULE_VIOLATION -> HttpStatus.BAD_REQUEST;
            case INVALID_STATE -> HttpStatus.CONFLICT;
            case POLICY_VIOLATION -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
