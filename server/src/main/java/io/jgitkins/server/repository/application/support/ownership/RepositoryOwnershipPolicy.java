package io.jgitkins.server.repository.application.support.ownership;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.shared.common.RepositoryPathHelper;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryOwnershipPolicy {

    private final RepositoryValidator repositoryValidator;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;

    public RepositoryCreationPlan prepareCreation(RepositoryCreateCommand command) {
        OwnerType ownerType = command.ownerType();
        OwnerId ownerId = resolveOwnerId(ownerType, command.organizeId());
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

    public void validateDeletion(Repository repository) {
        repositoryValidator.enforceDeletionPermission(repository);
    }

    private OwnerId resolveOwnerId(OwnerType ownerType, Long organizeId) {
        repositoryValidator.validateOwnership(ownerType, organizeId);
        if (ownerType == OwnerType.ORGANIZATION) {
            return OwnerId.of(organizeId);
        }
        return OwnerId.of(repositoryValidator.requireCurrentUserId());
    }

    public record RepositoryCreationPlan(Repository repository, InitialCommitOptions initialCommitOptions) {
    }
}
