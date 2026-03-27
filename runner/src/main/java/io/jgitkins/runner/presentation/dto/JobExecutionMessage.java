package io.jgitkins.runner.presentation.dto;

import lombok.Data;

@Data
public class JobExecutionMessage {
    private String repoUrl;
    private String taskCd;
    private String repoName;
    private String ref;
    private String jenkinsfilePath;
    private String dockerImage;
}
