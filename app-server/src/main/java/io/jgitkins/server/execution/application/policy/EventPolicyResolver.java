package io.jgitkins.server.execution.application.policy;

import io.jgitkins.server.shared.application.command.PushEventCommand;
import io.jgitkins.server.execution.application.contract.result.JobPlan;
import io.jgitkins.server.execution.application.contract.internal.PushJobPlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPolicyResolver {

    private final PushJobCreationPolicy pushJobCreationPolicy;

    public JobPlan resolvePushPlan(PushEventCommand command) {
        return pushJobCreationPolicy.plan(PushJobPlanRequest.from(command));
    }
}
