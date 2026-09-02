package io.jgitkins.server.execution.application.contract.internal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerExecutionConfig {
    private String runnerImageName;
    private String jenkinsPluginConfig;

    public static RunnerExecutionConfig defaultConfig() {
        return RunnerExecutionConfig.builder()
                .runnerImageName("jenkins/jenkinsfile-runner")
                .jenkinsPluginConfig("")
                .build();

    }

}
