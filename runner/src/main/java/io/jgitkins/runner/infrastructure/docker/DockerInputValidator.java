package io.jgitkins.runner.infrastructure.docker;

import io.jgitkins.runner.application.dto.JobRunContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DockerInputValidator {

    public void validate(JobRunContext context) {
        if (context == null || !StringUtils.hasText(context.getRunnerImageName())) {
            throw new IllegalArgumentException("Runner image name must be provided.");
        }
    }
}
