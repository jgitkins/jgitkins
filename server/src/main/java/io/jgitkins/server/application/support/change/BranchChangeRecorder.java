package io.jgitkins.server.application.support.change;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchChangeRecorder {

    private final BranchRepository branchRepository;

    public void record(PushEventCommand command) {
        Long repositoryId = command.getRepositoryId();
        if (repositoryId == null) {
            log.warn("push event branch state skipped: missing repository id. branch=[{}]", command.getBranchName());
            return;
        }

        if (command.isBranchCreated()) {
            log.info("Creating new branch [{}] for repository [{}]", command.getBranchName(), repositoryId);
            branchRepository.save(Branch.create(repositoryId, command.getBranchName()));
            return;
        }

        if (command.isBranchDeleted()) {
            log.info("Deleting branch [{}] from repository [{}]", command.getBranchName(), repositoryId);
            branchRepository.delete(Branch.create(repositoryId, command.getBranchName()));
        }
    }
}
