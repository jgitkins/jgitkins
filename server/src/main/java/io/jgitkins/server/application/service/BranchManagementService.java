package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.dto.command.BranchCreationContext;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.application.port.in.BranchDeleteUseCase;
import io.jgitkins.server.application.port.out.BranchGitPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.jgitkins.server.application.common.error.ApplicationErrorCode.REPOSITORY_NOT_FOUND;
import static io.jgitkins.server.application.common.error.ApplicationErrorCode.BRANCH_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BranchManagementService implements BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchCreationValidator branchCreationValidator;
    private final RepositoryAccessValidator repositoryAccessValidator;

    private final BranchGitPort branchGitPort;
    private final BranchRepository branchRepository;
    private final RepositoryPersistencePort repositoryPort;

    @Override
    @Transactional
    public void createBranch(BranchCreateCommand command) {
        BranchWriteContext context = loadWriteContext(command.repositoryId());
        Branch branch = Branch.create(command.repositoryId(), command.branchName());

        branchRepository.save(branch);

        String sourceBranch = branchCreationValidator.validateAndResolveSource(command, context.repository());
        BranchCreationContext creationContext = BranchCreationContext.of(
                command,
                context.namespace(),
                context.repository(),
                sourceBranch
        );

        branchGitPort.createBranch(creationContext);
    }

    @Override
    @Transactional
    public void deleteBranch(Long repositoryId, String branchName) {
        BranchWriteContext context = loadWriteContext(repositoryId);
        Branch branch = loadExistingBranch(repositoryId, branchName);

        branch.delete();
        branchRepository.delete(branch);
        branchGitPort.deleteBranch(
                context.namespace(),
                context.repository().getName().getValue(),
                branchName
        );
    }

    private BranchWriteContext loadWriteContext(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new ApplicationException(
                        REPOSITORY_NOT_FOUND,
                        "Repository not found: " + repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());
        return new BranchWriteContext(repository, namespace);
    }

    private Branch loadExistingBranch(Long repositoryId, String branchName) {
        return branchRepository.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new ApplicationException(
                        BRANCH_NOT_FOUND,
                        "Branch not found: " + branchName));
    }

    private record BranchWriteContext(Repository repository, String namespace) {
    }
}
