package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.dto.command.BranchCreationContext;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.mapper.BranchApplicationMapper;
import io.jgitkins.server.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.application.port.in.BranchDeleteUseCase;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.application.port.out.BranchGitPort;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchService implements BranchLoadUseCase, BranchCreateUseCase, BranchDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final BranchApplicationMapper branchApplicationMapper;
    private final BranchCreationValidator branchCreationValidator;
    private final RepositoryAccessValidator repositoryAccessValidator;

    private final BranchGitPort branchGitPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryPersistencePort repositoryPort;

    @Override
    public List<BranchSearchResult> getBranches(Long repositoryId) {
        return branchPort.findAllByRepositoryId(repositoryId)
                .stream()
                .map(branchApplicationMapper::toSearchResult)
                .toList();
    }

    @Override
    public BranchSearchResult getBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .map(branchApplicationMapper::toSearchResult)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRANCH_NOT_FOUND, "Branch not found: " + branchName));
    }

    @Override
    public void createBranch(BranchCreateCommand command) {
        Repository repository = loadRepositoryWithWriteAccess(command.getRepositoryId());
        String namespace = repositoryNamespaceResolver.resolve(repository);

        // 검증 및 소스 브랜치 결정 (Validator 위임)
        String resolvedSourceBranch = branchCreationValidator.validateAndResolveSource(command, repository);

        Branch newBranch = Branch.create(command.getRepositoryId(), command.getBranchName());
        BranchCreationContext context = BranchCreationContext.of(command, namespace, repository, resolvedSourceBranch);

        branchGitPort.createBranch(context);

        branchPort.save(newBranch);
    }

    @Override
    public void deleteBranch(Long repositoryId, String branchName) {
        Repository repository = loadRepositoryWithWriteAccess(repositoryId);
        String namespace = repositoryNamespaceResolver.resolve(repository);
        Branch branch = loadBranch(repositoryId, branchName);
        branchCreationValidator.validateNotDefaultBranch(repository, branch);

        branchGitPort.deleteBranch(namespace, repository.getName().getValue(), branchName);
        branchPort.deleteByRepositoryIdAndName(repositoryId, branchName);
    }

    private Branch loadBranch(Long repositoryId, String branchName) {
        return branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRANCH_NOT_FOUND, "Branch not found: " + branchName));
    }

    private Repository loadRepositoryWithWriteAccess(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND, "Repository not found: " + repositoryId));

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryAccessValidator.validateCanCommit(namespace, repository.getName().getValue());

        return repository;
    }
}
