package io.jgitkins.server.execution.application.support;

import io.jgitkins.server.execution.application.contract.internal.JobDispatchScope;
import io.jgitkins.server.execution.application.contract.internal.RunnerDispatchContext;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerDispatchContextResolver {

    private final RunnerRepository runnerRepository;

    public Optional<RunnerDispatchContext> resolve(String runnerToken) {
        if (runnerToken == null || runnerToken.isBlank()) {
            return Optional.empty();
        }

        return runnerRepository.findByToken(runnerToken)
                .map(this::toContext);
    }

    private RunnerDispatchContext toContext(Runner runner) {
        return new RunnerDispatchContext(
                runner.getId(),
                JobDispatchScope.valueOf(runner.getScopeType().name()),
                runner.getScopeTargetId()
        );
    }
}
