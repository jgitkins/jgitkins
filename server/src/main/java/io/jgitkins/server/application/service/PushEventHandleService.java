package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.JobCreateCommand;
import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobCreationDecision;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
import io.jgitkins.server.application.port.in.JobCreateUseCase;
import io.jgitkins.server.application.port.in.PushEventHandleUseCase;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.application.support.PushJobCreationPolicy;
import io.jgitkins.server.application.validate.JobCreationValidator;
import io.jgitkins.server.domain.Branch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j // TODO: rename 필요? 어쨌든 Push 이벤트가 호출됐을 때 PostHook 이 호출하는 서비스이다보니, 명칭에 Post 수식이 필요한지 검토 필요
public class PushEventHandleService implements PushEventHandleUseCase {

    private final JobCreateUseCase jobCreateUseCase;
    private final BranchPersistencePort branchPort;
    private final JobCreationValidator jobCreationValidator;
    private final PushJobCreationPolicy pushJobCreationPolicy;

    @Override
    @Transactional
    public void handle(PushEventCommand command) {
        log.debug("Handling push event for repositoryId=[{}], repoName=[{}]", command.getRepositoryId(), command.getRepoName());

        persistBranch(command.getRepositoryId(), command);

        JobCreationDecision decision = jobCreationValidator.validate(command);
        if (decision.isSkipped()) {
            log.info("push event job skipped: reason={}", decision.reason());
            return;
        }

        JobPlan jobPlan = pushJobCreationPolicy.plan(PushJobPlanRequest.from(command));
        if (jobPlan.isSkipped()) {
            log.info("push event job skipped: reason={}", jobPlan.getSkipReason());
            return;
        }

        jobCreateUseCase.create(buildJobCommand(command, jobPlan.getPipelineFilePath()));
    }

    private void persistBranch(Long repositoryId, PushEventCommand command) {
        if (repositoryId == null) {
            log.warn("push event branch state skipped: missing repository id. branch=[{}]", command.getBranchName());
            return;
        }

        if (command.isBranchCreated()) {
            log.info("Creating new branch [{}] for repository [{}]", command.getBranchName(), repositoryId);
            branchPort.save(Branch.create(repositoryId, command.getBranchName()));
        } else if (command.isBranchDeleted()) {
            log.info("Deleting branch [{}] from repository [{}]", command.getBranchName(), repositoryId);
            branchPort.deleteByRepositoryIdAndName(repositoryId, command.getBranchName());
        }
        // UPDATE(Push)의 경우 현재 로직에서는 별도의 Branch 엔티티 갱신이 필요 없음 (커밋 해시는 Job에 기록됨)
    }

    private JobCreateCommand buildJobCommand(PushEventCommand command, String pipelineFilePath) {
        return JobCreateCommand.builder()
                .repoName(command.getRepoName())
                .repositoryId(command.getRepositoryId())
                .branchName(command.getBranchName())
                .commitHash(command.getCommitHash())
                .pipelineFilePath(pipelineFilePath)
                .triggeredBy(command.getTriggeredBy())
                .build();
    }
}
