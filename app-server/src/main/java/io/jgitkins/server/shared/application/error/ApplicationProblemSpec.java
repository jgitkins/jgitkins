package io.jgitkins.server.shared.application.error;

import io.jgitkins.core.common.problem.ProblemSpec;

public enum ApplicationProblemSpec implements ProblemSpec<ApplicationErrorCode> {
    UNAUTHENTICATED(ApplicationErrorCode.UNAUTHENTICATED, "AUTH-001", "Authentication required", "auth.required"),
    ACCESS_DENIED(ApplicationErrorCode.ACCESS_DENIED, "AUTH-403", "Access denied", "auth.accessDenied"),
    ORGANIZE_ACCESS_DENIED(ApplicationErrorCode.ACCESS_DENIED, "ORG-403", "Access denied to organization", "organize.accessDenied"),
    REPOSITORY_ACCESS_DENIED(ApplicationErrorCode.ACCESS_DENIED, "REPO-403", "Access denied to repository", "repository.accessDenied"),
    USER_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "USER-404", "User not found", "user.notFound"),
    ORGANIZE_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "ORG-404", "Organization not found", "organize.notFound"),
    REPOSITORY_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "REPO-404", "Repository not found", "repository.notFound"),
    RUNNER_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "RUNNER-404", "Runner not found", "runner.notFound"),
    BRANCH_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "BRANCH-404", "Branch not found", "branch.notFound"),
    SOURCE_BRANCH_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "BRANCH-404-SRC", "Source branch not found", "branch.sourceNotFound"),
    COMMIT_NOT_FOUND(ApplicationErrorCode.NOT_FOUND, "COMMIT-404", "Commit not found", "commit.notFound"),
    USERNAME_ALREADY_EXISTS(ApplicationErrorCode.ALREADY_EXISTS, "USER-409-USERNAME", "Username already exists", "user.usernameAlreadyExists"),
    ORGANIZE_ALREADY_EXISTS(ApplicationErrorCode.ALREADY_EXISTS, "ORG-409", "Organization already exists", "organize.alreadyExists"),
    ORGANIZE_MEMBER_ALREADY_EXISTS(ApplicationErrorCode.ALREADY_EXISTS, "ORG-MEMBER-409", "Organization member already exists", "organize.memberAlreadyExists"),
    REPOSITORY_ALREADY_EXISTS(ApplicationErrorCode.ALREADY_EXISTS, "REPO-409", "Repository already exists", "repository.alreadyExists"),
    BRANCH_ALREADY_EXISTS(ApplicationErrorCode.ALREADY_EXISTS, "BRANCH-409", "Branch already exists", "branch.alreadyExists"),
    INVALID_NAMESPACE(ApplicationErrorCode.UNPROCESSABLE, "NAMESPACE-422", "Namespace is invalid", "namespace.invalid"),
    INVALID_OWNER_CONTEXT(ApplicationErrorCode.UNPROCESSABLE, "OWNER-CTX-422", "Owner context is invalid", "owner.contextInvalid"),
    MEMBER_IDENTIFIER_REQUIRED(ApplicationErrorCode.UNPROCESSABLE, "MEMBER-ID-422", "Member identifier is required", "member.identifierRequired"),
    REPOSITORY_NOT_INITIALIZED(ApplicationErrorCode.UNPROCESSABLE, "REPO-422-INIT", "Repository is not initialized", "repository.notInitialized");

    private final ApplicationErrorCode errorCode;
    private final String code;
    private final String defaultMessage;
    private final String messageKey;

    ApplicationProblemSpec(ApplicationErrorCode errorCode, String code, String defaultMessage, String messageKey) {
        this.errorCode = errorCode;
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.messageKey = messageKey;
    }

    @Override
    public ApplicationErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
