package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

/**
 * The requester is authenticated but is not an administrator.
 *
 * <p>Answers 403 rather than 404. The repository contexts hide a resource the caller cannot see,
 * because a 403 there would confirm that a private repository with that id exists. Nothing is hidden
 * here: {@code /api/admin/users} is published in the OpenAPI document, which is served to everyone,
 * so a 404 would conceal nothing and would only make a permission problem look like a routing bug.
 */
public class AdminPrivilegeRequiredException extends ApplicationException {

    public AdminPrivilegeRequiredException() {
        super(ApplicationProblemSpec.ACCESS_DENIED, "Administrator privilege is required");
    }
}
