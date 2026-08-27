package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.repository.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryManagementUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryRepository repositoryRepository;
    private final RepositoryOwnershipPolicy repositoryOwnershipPolicy;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        RepositoryCreationPlan creationPlan = repositoryOwnershipPolicy.prepareCreation(command);
        Repository saved = repositoryRepository.save(creationPlan.repository());
        Repository provisioned = repositoryProvisioner.provision(saved, creationPlan.initialCommitOptions());
        return repositoryApplicationMapper.toDto(provisioned);
    }

    @Override
    @Transactional
    public void deleteRepository(Long requesterUserId, Long repositoryId) {
        RepositoryId id = RepositoryId.of(repositoryId);
        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        // Not-found is decided before ownership, preserving the existing order. Checking ownership
        // first would let a caller distinguish "exists but not yours" from "does not exist", turning
        // the 404 into an existence oracle for private repositories.
        repositoryOwnershipPolicy.validateDeletion(requesterUserId, repository);
        repositoryProvisioner.delete(repository);
        repositoryRepository.deleteById(id);
    }
}
