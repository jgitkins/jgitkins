package io.jgitkins.server.infrastructure.common.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum InfrastructureErrorCode implements ErrorCode {
    INTERNAL_ERROR("INTERNAL_ERROR", "Infrastructure internal error"),
    PERSISTENCE_OPERATION_FAILED("PERSISTENCE_OPERATION_FAILED", "Persistence operation failed"),
    JGIT_OPERATION_FAILED("JGIT_OPERATION_FAILED", "JGit operation failed"),
    FILESYSTEM_ACCESS_FAILED("FILESYSTEM_ACCESS_FAILED", "Filesystem access failed"),
    REPOSITORY_CREATE_FAILED("REPOSITORY_CREATE_FAILED", "Repository Create Failed"),
    BRANCH_CREATE_FAILED("BRANCH_CREATE_FAILED", "Branch Create Failed"),
    BRANCH_DELETE_FAILED("BRANCH_DELETE_FAILED", "Branch Delete Failed"),
    BRANCH_LOAD_FAILED("BRANCH_LOAD_FAILED", "Branch Load Failed"),
    HEAD_POINT_FAILED("HEAD_POINT_FAILED", "Head Point Failed"),
    COMMIT_LOAD_FAILED("COMMIT_LOAD_FAILED", "Commit Load Failed"),
    COMMIT_CREATE_FAILED("COMMIT_CREATE_FAILED", "Commit Create Found"),
    COMMIT_TREE_LOAD_FAILED("COMMIT_TREE_LOAD_FAILED", "Commit Tree Load Failed"),
    FILE_LOAD_FAILED("FILE_LOAD_FAILED", "File Load Failed"),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "File Upload Failed"),
    RUNNER_DELETE_FAILED("RUNNER_DELETE_FAILED", "Runner Delete Failed"),
    RUNNER_ACTIVATION_FAILED("RUNNER_ACTIVATION_FAILED", "Runner Activation Failed"),
    COMMIT_FAILED("COMMIT_FAILED", "Commit failed"),
    REPOSITORY_DELETE_FAILED("REPOSITORY_DELETE_FAILED", "Repository Delete Failed");

    private final String code;
    private final String defaultMessage;

    InfrastructureErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
