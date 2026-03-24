package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.common.error.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ApplicationErrorHttpStatusMapper implements ErrorHttpStatusMapper {

    @Override
    public boolean supports(ErrorCode errorCode) {
        return errorCode instanceof ApplicationErrorCode;
    }

    @Override
    public HttpStatus map(ErrorCode errorCode) {
        return mapApplication((ApplicationErrorCode) errorCode);
    }

    private HttpStatus mapApplication(ApplicationErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED,
                    REPOSITORY_ACCESS_DENIED,
                    ORGANIZE_ACCESS_DENIED ->
                HttpStatus.FORBIDDEN;
            case ORGANIZE_NOT_FOUND,
                    USER_NOT_FOUND,
                    REPOSITORY_NOT_FOUND,
                    BRANCH_NOT_FOUND,
                    COMMIT_NOT_FOUND,
                    SOURCE_BRANCH_NOT_FOUND,
                    COMMIT_TREE_NOT_FOUND,
                    RUNNER_NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case REPOSITORY_ALREADY_EXISTS,
                    BRANCH_ALREADY_EXISTS,
                    ORGANIZE_ALREADY_EXISTS,
                    USERNAME_ALREADY_EXISTS,
                    RUNNER_ALREADY_ACTIVED ->
                HttpStatus.CONFLICT;
            case INVALID_OWNER_CONTEXT,
                    MEMBER_IDENTIFIER_REQUIRED,
                    REPOSITORY_NOT_INITIALIZED ->
                HttpStatus.UNPROCESSABLE_ENTITY;
            case FILE_READ_FAILED,
                    RUNNER_DELETE_FAILED,
                    RUNNER_ACTIVATION_FAILED ->
                HttpStatus.INTERNAL_SERVER_ERROR;
            case VALIDATION_FAILED,
                    INVALID_NAMESPACE ->
                HttpStatus.BAD_REQUEST;
        };
    }

}
