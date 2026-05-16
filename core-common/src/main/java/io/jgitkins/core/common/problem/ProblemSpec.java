package io.jgitkins.core.common.problem;

import io.jgitkins.core.common.error.ErrorCode;

public interface ProblemSpec<T extends ErrorCode> {

    T getErrorCode();

    String getCode();

    String getDefaultMessage();

    default String getMessageKey() {
        return getCode();
    }
}
