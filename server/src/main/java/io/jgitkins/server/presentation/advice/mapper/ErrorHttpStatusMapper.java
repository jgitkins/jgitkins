package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.core.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public interface ErrorHttpStatusMapper {

    boolean supports(ErrorCode errorCode);

    HttpStatus map(ErrorCode errorCode);
}

