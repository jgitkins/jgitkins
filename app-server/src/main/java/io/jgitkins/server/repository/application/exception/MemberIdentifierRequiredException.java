package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class MemberIdentifierRequiredException extends ApplicationException {

    public MemberIdentifierRequiredException(String message) {
        super(ApplicationProblemSpec.MEMBER_IDENTIFIER_REQUIRED, message);
    }
}
