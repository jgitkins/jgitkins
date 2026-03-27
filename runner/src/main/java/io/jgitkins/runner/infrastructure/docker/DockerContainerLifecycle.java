package io.jgitkins.runner.infrastructure.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerContainerLifecycle {

    private final DockerClient dockerClient;

    public String create(String image, List<Bind> binds) {
        CreateContainerCmd cmd = dockerClient.createContainerCmd(image);
        if (binds != null && !binds.isEmpty()) {
            cmd.withBinds(binds.toArray(new Bind[0]));
        }
        CreateContainerResponse response = cmd.exec();
        return response.getId();
    }

    public void start(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
    }

    public int waitForExit(String containerId) {
        Integer exitCode = dockerClient.waitContainerCmd(containerId)
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode();
        return exitCode != null ? exitCode : -1;
    }

    public void remove(String containerId) {
        if (!StringUtils.hasText(containerId)) {
            return;
        }
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
        } catch (RuntimeException ex) {
            log.warn("Failed to remove container {}", containerId, ex);
        }
    }
}
