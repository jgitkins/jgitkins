package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.application.exception.ApplicationException;

public class OrganizeMemberAlreadyExistsException extends ApplicationException {

    public OrganizeMemberAlreadyExistsException(Long organizeId, Long userId) {
        super(ApplicationProblemSpec.ORGANIZE_MEMBER_ALREADY_EXISTS,
                String.format("Organize member already exists: organizeId=%s, userId=%s", organizeId, userId));
    }
}
