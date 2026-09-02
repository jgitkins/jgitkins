package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

/**
 * The username a user asked for is already taken as a namespace.
 *
 * <p>Named for the question it answers. It used to be called {@code OrganizeAlreadyExistsException},
 * which collided with collaboration's exception of that name and misdescribed both: collaboration's
 * is thrown while creating an organization whose name is taken, and this one is thrown while
 * activating a user whose requested username is not available.
 *
 * <p>The two are not duplicates to merge. This context does not decide whether an organization name
 * is free -- it asks collaboration through {@code OrganizationNameUniquenessPort} and translates the
 * answer, which is what an anti-corruption layer is for. Reusing collaboration's exception here would
 * replace that translation with a direct dependency on another context's application layer.
 */
public class NamespaceAlreadyTakenException extends ApplicationException {
    public NamespaceAlreadyTakenException() {
        super(ApplicationProblemSpec.ORGANIZE_ALREADY_EXISTS, "Namespace already exists");
    }
}
