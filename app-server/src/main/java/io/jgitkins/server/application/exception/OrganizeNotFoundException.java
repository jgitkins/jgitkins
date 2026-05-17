package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

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
