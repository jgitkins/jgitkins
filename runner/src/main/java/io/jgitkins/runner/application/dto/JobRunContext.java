package io.jgitkins.runner.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JobRunContext {
    String workspacePath;
    String runnerImageName;
    String pluginPath;
}
