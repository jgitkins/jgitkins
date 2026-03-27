package io.jgitkins.runner.domain;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RunnerRuntimeConfig {

    String runnerToken;
    String restHost;
    Integer restPort;
    String restBasePath;
    String grpcHost;
    Integer grpcPort;
    Duration pollInterval;
    Duration busyWaitInterval;

    public boolean isLinked() {
        return runnerToken != null && !runnerToken.isBlank();
    }

    public boolean isReady() {
        return isLinked()
               && restHost != null && !restHost.isBlank()
               && restPort != null && restPort > 0
               && grpcHost != null && !grpcHost.isBlank()
               && grpcPort != null && grpcPort > 0;
    }

}
