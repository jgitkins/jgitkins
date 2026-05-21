package io.jgitkins.server.execution.application.service;

import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.contract.result.JobCreationDecision;
import io.jgitkins.server.execution.application.contract.result.JobPlan;
import io.jgitkins.server.execution.application.port.in.PushEventHandleUseCase;
import io.jgitkins.server.shared.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.execution.application.support.ExecutionRequestService;
import io.jgitkins.server.execution.application.policy.EventPolicyResolver;
import io.jgitkins.server.execution.application.validate.JobCreationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j // TODO: rename 필요? 어쨌든 Push 이벤트가 호출됐을 때 PostHook 이 호출하는 서비스이다보니, 명칭에 Post 수식이 필요한지 검토 필요
public class PushEventHandleService implements PushEventHandleUseCase {

    private final BranchChangeRecorder branchChangeRecorder;
    private final JobCreationValidator jobCreationValidator;
    private final EventPolicyResolver eventPolicyResolver;
    private final ExecutionRequestService executionRequestService;

    @Override
    @Transactional
    public void handle(PushEventCommand command) {
        
        // Branch 영속화처리 (내부 Application 처리해도되지 않을까..)
        log.debug("Handling push event for repositoryId=[{}], repoName=[{}]", command.getRepositoryId(), command.getRepoName());
        branchChangeRecorder.record(command);

        JobCreationDecision decision = jobCreationValidator.validate(command);
        if (decision.isSkipped()) {
            log.info("push event job skipped: reason={}", decision.reason());
            return;
        }

        JobPlan jobPlan = eventPolicyResolver.resolvePushPlan(command);
        if (jobPlan.isSkipped()) {
            log.info("push event job skipped: reason={}", jobPlan.getSkipReason());
            return;
        }

        executionRequestService.requestPushExecution(command, jobPlan);
    }
}
