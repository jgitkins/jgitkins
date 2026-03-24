package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchCreationValidator {

    private final BranchPersistencePort branchPort;

    /**
     * 브랜치 생성에 필요한 모든 비즈니스 규칙을 검증하고 소스 브랜치를 결정합니다.
     */
    public String validateAndResolveSource(BranchCreateCommand command, Repository repository) {
        validateRepositoryInitialized(repository);
        validateBranchDoesNotExist(command.getRepositoryId(), command.getBranchName());
        return resolveAndValidateSourceBranch(command, repository);
    }

    public void validateBranchDoesNotExist(Long repositoryId, String branchName) {
        branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .ifPresent(existing -> {
                    throw new ApplicationException(ApplicationErrorCode.BRANCH_ALREADY_EXISTS);
                });
    }

    public void validateRepositoryInitialized(Repository repository) {
        if (!repository.isInitialized()) {
            throw new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_INITIALIZED,
                    "Repository is not yet initialized. Initialize default branch before creating new branches.");
        }
    }

    public String resolveAndValidateSourceBranch(BranchCreateCommand command, Repository repository) {
        String sourceBranch = (command.getSourceBranch() == null || command.getSourceBranch().isBlank())
                ? repository.getDefaultBranch().getValue()
                : command.getSourceBranch();

        branchPort.findByRepositoryIdAndName(repository.getId().getValue(), sourceBranch)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SOURCE_BRANCH_NOT_FOUND,
                        "Source branch not found or not initialized: " + sourceBranch));
        return sourceBranch;
    }

    public void validateNotDefaultBranch(Repository repository, Branch branch) {
        if (repository.getDefaultBranch().getValue().equals(branch.getName()) || branch.isDefaultBranch()) {
            throw new ApplicationException(ApplicationErrorCode.BRANCH_ALREADY_EXISTS,
                    "Default branch cannot be deleted: " + branch.getName());
        }
    }
}
