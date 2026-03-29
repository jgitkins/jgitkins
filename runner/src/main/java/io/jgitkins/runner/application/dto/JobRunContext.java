package io.jgitkins.runner.application.dto;

public record JobRunContext(
        String workspacePath,
        String runnerImageName,
        String pluginPath
) {
}
