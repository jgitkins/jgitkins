package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.repository.BranchRepository;
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
        branchGitPort.createBranch(creationContext);
        return branch;
    }
}
