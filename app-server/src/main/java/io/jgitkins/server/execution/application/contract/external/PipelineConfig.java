package io.jgitkins.server.execution.application.contract.external;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import io.jgitkins.server.execution.application.contract.internal.PipelineRule;

@Getter
@RequiredArgsConstructor
public class PipelineConfig {

    private final List<PipelineRule> rules;

    public Optional<PipelineRule> findRule(String branchName) {
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }

        for (PipelineRule rule : rules) {
            if (rule.matches(branchName)) {
                return Optional.of(rule);
            }
        }

        return Optional.empty();
    }
}
