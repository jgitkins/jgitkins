package io.jgitkins.server.common.problem;

import io.jgitkins.server.common.error.ErrorCode;

public interface ProblemSpec<T extends ErrorCode> {

    T getErrorCode();

    String getCode();

    String getDefaultMessage();

    default String getMessageKey() {
        return getCode();
    }
}
