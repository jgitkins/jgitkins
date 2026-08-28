package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

/**
 * The organization still owns repositories, so deleting it would orphan them.
 *
 * <p>The schema declares no foreign keys, so deleting the ORGANIZE row leaves every repository with
 * {@code OWNER_TYPE='ORGANIZATION'} pointing at an id that no longer resolves. A new organization
 * gets a new id, so there is no way back: the repositories cannot be re-attached and nobody can be
 * shown as their owner.
 *
 * <p>Refusing is the honest answer while deletion is immediate. The direction recorded in TODOS.md
 * is GitLab-style delayed deletion, where the request marks the organization and a sweeper removes
 * the owned resources after a retention window. **That work removes this guard** -- if it is still
 * here afterwards, the transition is not finished.
 */
public class OrganizeHasRepositoriesException extends ApplicationException {

    public OrganizeHasRepositoriesException(long repositoryCount) {
        super(ApplicationProblemSpec.ORGANIZE_HAS_REPOSITORIES,
                "Organization still owns " + repositoryCount
                        + " repositor" + (repositoryCount == 1 ? "y" : "ies")
                        + ". Transfer or delete them first.");
    }
}
