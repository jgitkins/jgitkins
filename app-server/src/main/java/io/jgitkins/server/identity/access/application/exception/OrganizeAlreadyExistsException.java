package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

public class OrganizeAlreadyExistsException extends ApplicationException {
    public OrganizeAlreadyExistsException() {
        super(ApplicationProblemSpec.ORGANIZE_ALREADY_EXISTS, "Namespace already exists");
    }
}
