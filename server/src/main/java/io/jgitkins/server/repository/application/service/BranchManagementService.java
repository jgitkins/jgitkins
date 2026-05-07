package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchManagementUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryAccessValidator repositoryAccessValidator;
    private final RepositoryRepository repositoryRepository;
    private final BranchFactory branchFactory;
    private final BranchGitPort branchGitPort;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        BranchRepositoryContext context = loadWriteContext(command.repositoryId());
        branchFactory.create(command, context.namespace(), context.repository());
    }

    @Override
    @Transactional
    public void deleteBranch(Long repositoryId, String branchName) {
        BranchRepositoryContext context = loadWriteContext(repositoryId);
        Branch branch = loadExistingBranch(repositoryId, branchName);

        branch.delete();
        branchRepository.delete(branch);
        branchGitPort.deleteBranch(
                context.namespace(),
                context.repository().getName().getValue(),
                branchName
        );
    }

    private BranchRepositoryContext loadWriteContext(Long repositoryId) {
        Repository repository = repositoryRepository.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchRepositoryContext(repository, namespace);
    }

    private Branch loadExistingBranch(Long repositoryId, String branchName) {
        return branchRepository.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new BranchNotFoundException(branchName));
    }

    private record BranchRepositoryContext(Repository repository, String namespace) {
    }
}
