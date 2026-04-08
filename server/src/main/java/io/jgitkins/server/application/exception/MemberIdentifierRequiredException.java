package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class MemberIdentifierRequiredException extends ApplicationException {

    public MemberIdentifierRequiredException(String message) {
        super(ApplicationProblemSpec.MEMBER_IDENTIFIER_REQUIRED, message);
    }
}
