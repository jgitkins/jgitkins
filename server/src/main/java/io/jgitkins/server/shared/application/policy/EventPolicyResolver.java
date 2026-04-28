package io.jgitkins.server.shared.application.policy;

import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.result.JobPlan;
import io.jgitkins.server.application.dto.support.PushJobPlanRequest;
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
