package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.repository.application.validate.BranchCreationValidator;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.exception.BranchAlreadyExistsException;
import io.jgitkins.server.repository.application.exception.SourceBranchNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefAlreadyExistsException;
import io.jgitkins.server.repository.application.port.out.exception.GitSourceBranchRefMissingException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchFactory {

    private final BranchCreationValidator branchCreationValidator;
    private final BranchRepository branchRepository;
    private final BranchGitPort branchGitPort;

    public Branch create(
            BranchCreateCommand command,
            String namespace,
            Repository repository
    ) {
        String sourceBranch = branchCreationValidator.validateAndResolveSource(command, repository);
        Branch branch = Branch.create(command.repositoryId(), command.branchName());
        BranchCreationContext creationContext = BranchCreationContext.of(
                command,
                namespace,
                repository,
                sourceBranch
        );

        branchRepository.save(branch);
        try {
            branchGitPort.createBranch(creationContext);
        } catch (GitSourceBranchRefMissingException e) {
            throw new SourceBranchNotFoundException(e.getBranchName());
        } catch (GitBranchRefAlreadyExistsException e) {
            throw new BranchAlreadyExistsException(e.getBranchName());
        }
        return branch;
    }
}
