package io.jgitkins.server.application.validate;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.BranchAlreadyExistsException;
import io.jgitkins.server.application.exception.RepositoryNotInitializedException;
import io.jgitkins.server.repository.application.exception.SourceBranchNotFoundException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchCreationValidator {

    private final BranchRepository branchPort;

    /**
     * 브랜치 생성에 필요한 모든 비즈니스 규칙을 검증하고 소스 브랜치를 결정합니다.
     */
    public String validateAndResolveSource(BranchCreateCommand command, Repository repository) {
        validateRepositoryInitialized(repository);
        validateBranchDoesNotExist(command.repositoryId(), command.branchName());
        return resolveAndValidateSourceBranch(command, repository);
    }

    public void validateBranchDoesNotExist(Long repositoryId, String branchName) {
        branchPort.findByRepositoryIdAndName(repositoryId, branchName)
                .ifPresent(existing -> {
                    throw new BranchAlreadyExistsException(branchName);
                });
    }

    public void validateRepositoryInitialized(Repository repository) {
        if (!repository.isInitialized()) {
            throw new RepositoryNotInitializedException(
                    "Repository is not yet initialized. Initialize default branch before creating new branches.");
        }
    }

    public String resolveAndValidateSourceBranch(BranchCreateCommand command, Repository repository) {
        String sourceBranch = (command.sourceBranch() == null || command.sourceBranch().isBlank())
                ? repository.getDefaultBranch().getValue()
                : command.sourceBranch();

        branchPort.findByRepositoryIdAndName(repository.getId().getValue(), sourceBranch)
                .orElseThrow(() -> new SourceBranchNotFoundException(
                        "Source branch not found or not initialized: " + sourceBranch, true));
        return sourceBranch;
    }

}
