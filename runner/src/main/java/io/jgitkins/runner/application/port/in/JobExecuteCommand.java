package io.jgitkins.runner.application.port.in;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JobExecuteCommand {
    String repoUrl;
    String taskCd;
    String repoName;
    String ref;
    String jenkinsfilePath;
    String dockerImage;
}
