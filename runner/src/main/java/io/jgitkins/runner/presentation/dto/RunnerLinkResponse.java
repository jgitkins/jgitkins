package io.jgitkins.runner.presentation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RunnerLinkResponse {
    String message;
    String serverHost;
    int serverPort;
    String defaultDockerImage;
    String defaultJenkinsfilePath;
}
