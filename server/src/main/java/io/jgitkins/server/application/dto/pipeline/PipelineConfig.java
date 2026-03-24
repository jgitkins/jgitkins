package io.jgitkins.server.application.dto.pipeline;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
