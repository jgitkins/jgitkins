package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.application.exception.ApplicationException;

public class OrganizeNotFoundException extends ApplicationException {

    public OrganizeNotFoundException() {
        super(ApplicationProblemSpec.ORGANIZE_NOT_FOUND, "Organize not found");
    }

    public OrganizeNotFoundException(Long organizeId) {
        super(ApplicationProblemSpec.ORGANIZE_NOT_FOUND, "Organize not found: " + organizeId);
    }

    public OrganizeNotFoundException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_NOT_FOUND, message);
    }
}
