package io.jgitkins.server.application.common.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum ApplicationErrorCode implements ErrorCode {
    // 401 Unauthorized
    UNAUTHENTICATED("UNAUTHENTICATED", "Authentication required"),

    // 403 Forbidden
    ACCESS_DENIED("ACCESS_DENIED", "Access denied"),
    REPOSITORY_ACCESS_DENIED("REPOSITORY_ACCESS_DENIED", "You do not have access to this repository"),
    ORGANIZE_ACCESS_DENIED("ORGANIZE_ACCESS_DENIED", "You are not a member of this organization"),

    // 404 Not Found
    ORGANIZE_NOT_FOUND("ORGANIZE_NOT_FOUND", "Organize Not Found"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User Not Found"),
    REPOSITORY_NOT_FOUND("REPOSITORY_NOT_FOUND", "Source Repository Not Found"),
    BRANCH_NOT_FOUND("BRANCH_NOT_FOUND", "Branch Not Found"),
    COMMIT_NOT_FOUND("COMMIT_NOT_FOUND", "Commit Not Found"),
    SOURCE_BRANCH_NOT_FOUND("SOURCE_BRANCH_NOT_FOUND", "Source Branch Not Found"),
    COMMIT_TREE_NOT_FOUND("COMMIT_TREE_NOT_FOUND", "Commit Tree Not Found"),
    RUNNER_NOT_FOUND("RUNNER_NOT_FOUND", "Runner Not Found"),

    // 409 Conflict
    REPOSITORY_ALREADY_EXISTS("REPOSITORY_ALREADY_EXISTS", "Repository Already Exists"),
    BRANCH_ALREADY_EXISTS("BRANCH_ALREADY_EXISTS", "Branch Already Exists"),
    ORGANIZE_ALREADY_EXISTS("ORGANIZE_ALREADY_EXISTS", "Organize Already Exists"),
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "Username Already Exists"),
    RUNNER_ALREADY_ACTIVED("RUNNER_ALREADY_ACTIVED", "Runner Already Active"),

    // 422 Unprocessable Entity (Logical/Semantic Errors)
    INVALID_OWNER_CONTEXT("INVALID_OWNER_CONTEXT", "Invalid owner type or ID combination"),
    MEMBER_IDENTIFIER_REQUIRED("MEMBER_IDENTIFIER_REQUIRED", "Member identifier (User ID or Repository ID) is missing"),
    REPOSITORY_NOT_INITIALIZED("REPOSITORY_NOT_INITIALIZED", "Repository is not yet initialized"),

    FILE_READ_FAILED("FILE_READ_FAILED", "Failed to read file content"),
    RUNNER_DELETE_FAILED("RUNNER_DELETE_FAILED", "Runner Delete Failed"),

    RUNNER_ACTIVATION_FAILED("RUNNER_ACTIVATION_FAILED", "Runner Activation Failed"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    INVALID_NAMESPACE("INVALID_NAMESPACE", "Invalid namespace");

    private final String code;
    private final String defaultMessage;

    ApplicationErrorCode(String code, String defaultMessage) {
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
