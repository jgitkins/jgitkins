package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

public class OrganizeMemberNotFoundException extends ApplicationException {

    public OrganizeMemberNotFoundException(Long organizeId, Long userId) {
        super(ApplicationProblemSpec.ORGANIZE_MEMBER_NOT_FOUND,
                "Organization member not found: organizeId=" + organizeId + ", userId=" + userId);
    }
}