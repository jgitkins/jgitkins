package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobCreationDecision;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.port.in.PushEventHandleUseCase;
import io.jgitkins.server.application.support.change.BranchChangeRecorder;
import io.jgitkins.server.application.support.execution.ExecutionRequestService;
import io.jgitkins.server.application.support.policy.EventPolicyResolver;
import io.jgitkins.server.application.validate.JobCreationValidator;
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
