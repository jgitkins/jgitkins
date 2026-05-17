package io.jgitkins.runner.infrastructure.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerLogStreamer {

    private final DockerClient dockerClient;

    public ResultCallback<Frame> stream(String containerId) {
        return dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        log.info("[RUNNER LOG] {}", new String(frame.getPayload()));
                    }
                });
    }
}
