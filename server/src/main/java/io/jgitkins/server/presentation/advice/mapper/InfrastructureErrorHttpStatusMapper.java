package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class InfrastructureErrorHttpStatusMapper implements ErrorHttpStatusMapper {

    @Override
    public boolean supports(ErrorCode errorCode) {
        return errorCode instanceof InfrastructureErrorCode;
    }

    @Override
    public HttpStatus map(ErrorCode errorCode) {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

