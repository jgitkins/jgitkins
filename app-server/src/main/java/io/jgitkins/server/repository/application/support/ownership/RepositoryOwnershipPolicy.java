package io.jgitkins.server.repository.application.support.ownership;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.policy.RepositoryDeletionPolicy;
import io.jgitkins.server.repository.application.validate.RepositoryValidator;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.repository.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryOwnershipPolicy {

    private final RepositoryValidator repositoryValidator;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryDeletionPolicy repositoryDeletionPolicy;

    public RepositoryCreationPlan prepareCreation(RepositoryCreateCommand command) {
        OwnerType ownerType = command.ownerType();
        OwnerId ownerId = resolveOwnerId(command.requesterUserId(), ownerType, command.organizeId());
        RepositoryName repositoryName = RepositoryName.from(command.repoName());

        repositoryValidator.validateRepositoryNameUnique(ownerType, ownerId, repositoryName);

        String namespace = repositoryNamespaceResolver.resolve(ownerType, ownerId);
        InitialCommitOptions initialCommitOptions = InitialCommitOptions.of(
                command.readme(),
                command.message(),
                command.authorName(),
                command.authorEmail()
        );

        Repository repository = Repository.create(
                ownerType,
                ownerId,
                repositoryName,
                RepositoryPath.from(command.repoName()),
                BranchName.of(command.mainBranch()),
                command.visibility() != null ? command.visibility() : RepositoryVisibility.PRIVATE,
                command.description(),
                RepositoryPathHelper.buildClonePath(namespace, command.repoName()),
                command.credentialId(),
                initialCommitOptions.requiresInitialContent()
        );

        return new RepositoryCreationPlan(repository, initialCommitOptions);
    }

    public void validateDeletion(Long requesterUserId, Repository repository) {
        repositoryDeletionPolicy.validateCanDelete(requesterUserId, repository);
    }

    private OwnerId resolveOwnerId(Long requesterUserId, OwnerType ownerType, Long organizeId) {
        repositoryValidator.validateOwnership(requesterUserId, ownerType, organizeId);
        if (ownerType == OwnerType.ORGANIZATION) {
            return OwnerId.of(organizeId);
        }
        return OwnerId.of(repositoryValidator.requireRequesterId(requesterUserId));
    }
}
