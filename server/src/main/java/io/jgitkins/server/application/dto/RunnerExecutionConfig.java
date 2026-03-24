package io.jgitkins.server.application.dto;

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
