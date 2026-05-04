package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryDeleteUseCase;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.contract.internal.RepositoryCreationPlan;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryCreateUseCase, RepositoryDeleteUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryOwnershipPolicy repositoryOwnershipPolicy;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        RepositoryCreationPlan creationPlan = repositoryOwnershipPolicy.prepareCreation(command);
        Repository saved = repositoryPort.save(creationPlan.repository());
        Repository provisioned = repositoryProvisioner.provision(saved, creationPlan.initialCommitOptions());
        return repositoryApplicationMapper.toDto(provisioned);
    }

    @Override
    @Transactional
    public void deleteRepository(Long repositoryId) {
        RepositoryId id = RepositoryId.of(repositoryId);
        Repository repository = repositoryPort.findById(id)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        repositoryOwnershipPolicy.validateDeletion(repository);
        repositoryProvisioner.delete(repository);
        repositoryPort.deleteById(id);
    }
}
