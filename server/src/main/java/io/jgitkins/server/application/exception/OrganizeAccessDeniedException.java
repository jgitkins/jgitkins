package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class OrganizeAccessDeniedException extends ApplicationException {

    public OrganizeAccessDeniedException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_ACCESS_DENIED, message);
    }
}
