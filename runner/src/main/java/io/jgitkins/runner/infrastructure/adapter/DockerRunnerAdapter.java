package io.jgitkins.runner.infrastructure.adapter;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import io.jgitkins.runner.application.dto.JobRunContext;
import io.jgitkins.runner.application.port.out.JobRunnerPort;
import io.jgitkins.runner.infrastructure.docker.DockerBindBuilder;
import io.jgitkins.runner.infrastructure.docker.DockerContainerLifecycle;
import io.jgitkins.runner.infrastructure.docker.DockerInputValidator;
import io.jgitkins.runner.infrastructure.docker.DockerLogStreamer;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerRunnerAdapter implements JobRunnerPort {

    private final DockerInputValidator inputValidator;
    private final DockerBindBuilder bindBuilder;
    private final DockerLogStreamer logStreamer;
    private final DockerContainerLifecycle lifecycle;

    @Override
    public int run(JobRunContext context) {
        inputValidator.validate(context);
        String workspacePath = context.getWorkspacePath();
        String imageName = context.getRunnerImageName();
        String pluginPath = context.getPluginPath();

        logContainerInfo(imageName, workspacePath, pluginPath);

        List<Bind> binds = bindBuilder.build(workspacePath, pluginPath);
        String containerId = lifecycle.create(imageName, binds);

        try {
            lifecycle.start(containerId);
            try (ResultCallback<Frame> logs = logStreamer.stream(containerId)) {
                int exitCode = lifecycle.waitForExit(containerId);
                log.info("Jenkinsfile Runner exit code: {}", exitCode);
                return exitCode;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lifecycle.remove(containerId);
        }
    }

    private void logContainerInfo(String image, String repositoryPath, String pluginPath) {
        log.info("Running Jenkinsfile Runner container: {}", image);
        if (StringUtils.hasText(repositoryPath)) {
            log.info("Workspace: {}", repositoryPath);
        } else {
            log.warn("Workspace path is not provided. Container will be started without workspace bind.");
        }
        if (StringUtils.hasText(pluginPath)) {
            log.info("Plugin config: {}", pluginPath);
        }
    }
}
