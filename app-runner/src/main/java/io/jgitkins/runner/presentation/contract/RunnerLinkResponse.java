package io.jgitkins.runner.presentation.contract;

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
