package io.jgitkins.runner.application.contract;

public record JobRunContext(
        String workspacePath,
        String runnerImageName,
        String pluginPath
) {
}
