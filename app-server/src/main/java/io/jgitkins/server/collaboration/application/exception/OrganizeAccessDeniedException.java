package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.application.exception.ApplicationException;

public class OrganizeAccessDeniedException extends ApplicationException {

    public OrganizeAccessDeniedException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_ACCESS_DENIED, message);
    }
}
