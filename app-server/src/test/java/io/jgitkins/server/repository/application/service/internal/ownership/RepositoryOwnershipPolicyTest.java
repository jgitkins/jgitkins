package io.jgitkins.server.repository.application.service.internal.ownership;

import static org.mockito.Mockito.verify;

import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.repository.application.policy.RepositoryDeletionPolicy;
import io.jgitkins.server.repository.application.validate.RepositoryValidator;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The ownership policy must pass the requester it was given straight through.
 *
 * <p>This is the seam where task 2.64's explicit actor could silently go missing. The policy sits between
 * the service and the validator, and before this task the validator fetched the actor itself — so a
 * policy that dropped its {@code requesterUserId} argument would still compile, and the validator would
 * still authorize, just against whoever the security context happened to hold. That is exactly the
 * behaviour the task removes, and it would be invisible to any test that only checked the outcome.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryOwnershipPolicyTest {

    @Mock
    private RepositoryValidator repositoryValidator;

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;

    @Mock
    private RepositoryDeletionPolicy repositoryDeletionPolicy;

    @Test
    void validateDeletion_passesExplicitRequesterToValidator() {
        RepositoryOwnershipPolicy policy =
                new RepositoryOwnershipPolicy(
                        repositoryValidator, repositoryNamespaceResolver, repositoryDeletionPolicy);
        Repository repository = userOwnedRepository();

        policy.validateDeletion(7L, repository);

        // The exact argument, not any(): passing the wrong requester through would authorize a
        // deletion for someone other than the caller and still look like a working delegation.
        verify(repositoryDeletionPolicy).validateCanDelete(7L, repository);
    }

    @Test
    void validateDeletion_forwardsANullRequesterRatherThanSubstitutingOne() {
        RepositoryOwnershipPolicy policy =
                new RepositoryOwnershipPolicy(
                        repositoryValidator, repositoryNamespaceResolver, repositoryDeletionPolicy);
        Repository repository = userOwnedRepository();

        policy.validateDeletion(null, repository);

        // Forwarded, not defaulted. The validator owns the rejection, and a policy that substituted a
        // fallback actor here would authorize a deletion for whoever that fallback named.
        verify(repositoryDeletionPolicy).validateCanDelete(null, repository);
    }

    private static Repository userOwnedRepository() {
        return Repository.rehydrate(
                io.jgitkins.server.repository.domain.vo.RepositoryId.of(1L),
                OwnerType.USER,
                RepositoryOwnerId.of(7L),
                RepositoryName.from("repo"),
                RepositoryPath.from("repo"),
                BranchName.of("main"),
                RepositoryVisibility.PRIVATE,
                null, "/alice/repo.git", null, null, null, null);
    }
}
