package io.jgitkins.server.architecture;

import io.jgitkins.server.change.review.adapter.in.rest.MergeController;
import io.jgitkins.server.change.review.adapter.in.rest.PullRequestController;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeController;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberController;
import io.jgitkins.server.collaboration.adapter.in.web.WebOrganizeController;
import io.jgitkins.server.execution.adapter.in.rest.RunnerController;
import io.jgitkins.server.identity.access.adapter.in.rest.AdminUserController;
import io.jgitkins.server.identity.access.adapter.in.rest.OAuthController;
import io.jgitkins.server.identity.access.adapter.in.rest.SignupController;
import io.jgitkins.server.identity.access.adapter.in.rest.UserController;
import io.jgitkins.server.identity.access.adapter.in.rest.UserCredentialController;
import io.jgitkins.server.repository.adapter.in.rest.BranchController;
import io.jgitkins.server.repository.adapter.in.rest.RepositoryCommitController;
import io.jgitkins.server.repository.adapter.in.rest.RepositoryContentController;
import io.jgitkins.server.repository.adapter.in.rest.RepositoryFileController;
import io.jgitkins.server.repository.adapter.in.rest.RepositoryManagementController;
import io.jgitkins.server.repository.adapter.in.rest.RepositoryMemberController;
import io.jgitkins.server.repository.adapter.in.web.WebRepositoryController;
import java.util.List;

/**
 * Every Spring MVC controller in app-server, named once.
 *
 * <p>This list used to live inline inside a {@code @Test} body in
 * {@code ArchitecturePackageConventionTest}, where nothing could read it and nothing checked it was
 * complete. The proof that nobody read it: {@code MergeController} was listed twice in the same
 * {@code List.of} -- once short, once fully qualified -- so nineteen entries named eighteen classes.
 * A controller added and not listed here was simply not checked, and the suite stayed green.
 *
 * <p>It lives in this package, next to {@link ArchitectureScanner}, so
 * {@link ControllerAllowlistCompletenessTest} can hold it against the source tree using the
 * scanner's package-private API. {@code ArchitecturePackageConventionTest} consumes it from
 * {@code io.jgitkins.server.application}; that is why the type is public.
 *
 * <p>Adding a controller and forgetting this list now fails the build with the missing class named.
 */
public final class ControllerInventory {

    private ControllerInventory() {
    }

    public static final List<Class<?>> ALL = List.of(
            AdminUserController.class,
            BranchController.class,
            MergeController.class,
            OAuthController.class,
            OrganizeController.class,
            OrganizeMemberController.class,
            PullRequestController.class,
            RepositoryCommitController.class,
            RepositoryContentController.class,
            RepositoryFileController.class,
            RepositoryManagementController.class,
            RepositoryMemberController.class,
            RunnerController.class,
            SignupController.class,
            UserController.class,
            UserCredentialController.class,
            WebOrganizeController.class,
            WebRepositoryController.class);
}
