package io.jgitkins.server.common.presentation.advice.translator;

import io.jgitkins.core.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public interface ErrorHttpStatusMapper {

    boolean supports(ErrorCode errorCode);

    HttpStatus map(ErrorCode errorCode);
}

