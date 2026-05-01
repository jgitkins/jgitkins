package io.jgitkins.server.repository.application.support.branch;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchWritePolicy {

    private final BranchCreationValidator branchCreationValidator;

    public Branch createBranchMetadata(BranchCreateCommand command) {
        return Branch.create(command.repositoryId(), command.branchName());
    }

    public String resolveSourceBranch(BranchCreateCommand command, Repository repository) {
        return branchCreationValidator.validateAndResolveSource(command, repository);
    }
}
