package io.jgitkins.runner.application.exception;

public enum ErrorCode {



    /***
     * Branches
     */

    BAD_REQUEST("BAD_REQUEST", "BAD_REQUEST")
    ,  REPOSITORY_NOT_FOUND("REPOSITORY_NOT_FOUND", "Source Repository Not Found")
    , REPOSITORY_CREATE_FAILED("REPOSITORY_CREATE_FAILED", "Repository Create Failed")
    , REPOSITORY_ALREADY_EXISTS("REPOSITORY_ALREADY_EXISTS", "Repository Already Exists")
    , SOURCE_BRANCH_NOT_FOUND("SOURCE_BRANCH_NOT_FOUND", "Source Branch Not Found")
    , BRANCH_ALREADY_EXISTS("BRANCH_ALREADY_EXISTS", "Branch Already Exists")
    , BRANCH_CREATE_FAILED("BRANCH_CREATE_FAILED", "Branch Create Failed")
    , BRANCH_DELETE_FAILED("BRANCH_DELETE_FAILED", "Branch Delete Failed")
    , BRANCH_NOT_FOUND("BRANCH_NOT_FOUND", "Branch Not Found")
    , BRANCH_LOAD_FAILED("BRANCH_LOAD_FAILED", "Branch Load Failed")
    , HEAD_POINT_FAILED("HEAD_POINT_FAILED", "Head Point Failed")
    , COMMIT_NOT_FOUND("COMMIT_NOT_FOUND", "Commit Not Found")
    , COMMIT_LOAD_FAILED("COMMIT_LOAD_FAILED", "Commit Load Failed")
    , COMMIT_CREATE_FAILED("COMMIT_CREATE_FAILED", "Commit Create Found")
    , COMMIT_TREE_NOT_FOUND("COMMIT_TREE_NOT_FOUND", "Commit Tree Not Found")
    , COMMIT_TREE_LOAD_FAILED("COMMIT_TREE_LOAD_FAILED", "Commit Tree Load Failed")
    , FILE_LOAD_FAILED("FILE_LOAD_FAILED", "File Load Failed")
    , INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Server Error Occur")

    , RUNNER_NOT_FOUND("RUNNER_NOT_FOUND", "Runner Not Found")
    , RUNNER_DELETE_FAILED("RUNNER_DELETE_FAILED", "Runner Delete Failed")
    , RUNNER_ACTIVATION_FAILED("RUNNER_ACTIVATION_FAILED", "Runner Activation Failed")
    , ORGANIZE_NOT_FOUND("ORGANIZE_NOT_FOUND", "Organize Not Found")
    , ORGANIZE_ALREADY_EXISTS("ORGANIZE_ALREADY_EXISTS", "Organize Already Exists")
    , REPOSITORY_DELETE_FAILED("REPOSITORY_DELETE_FAILED", "Repository Delete Failed")
    ;

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
