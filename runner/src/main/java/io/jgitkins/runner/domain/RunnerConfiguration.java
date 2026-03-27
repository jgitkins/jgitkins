package io.jgitkins.runner.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@ToString
public class RunnerConfiguration {

    RunnerRuntimeConfig runtimeConfig;
    RunnerExecutionConfig executionConfig;

    public boolean isLinked() {
        return runtimeConfig != null && runtimeConfig.isLinked();
    }

    public boolean isReadyForScheduling() {
        return runtimeConfig != null && runtimeConfig.isReady();
    }

    public String runnerToken() {
        return runtimeConfig != null ? runtimeConfig.getRunnerToken() : null;
    }

    public String restHost() {
        return runtimeConfig != null ? runtimeConfig.getRestHost() : null;
    }

    public Integer restPort() {
        return runtimeConfig != null ? runtimeConfig.getRestPort() : null;
    }

    public String restBasePath() {
        return runtimeConfig != null ? runtimeConfig.getRestBasePath() : null;
    }

    public String grpcHost() {
        return runtimeConfig != null ? runtimeConfig.getGrpcHost() : null;
    }

    public Integer grpcPort() {
        return runtimeConfig != null ? runtimeConfig.getGrpcPort() : null;
    }

    public String serverAuthority() {
        if (runtimeConfig == null || runtimeConfig.getRestHost() == null) {
            return null;
        }
        Integer port = runtimeConfig.getRestPort();
        return port != null && port > 0 ? runtimeConfig.getRestHost() + ":" + port : runtimeConfig.getRestHost();
    }

    public RunnerRuntimeConfig runtime() {
        return runtimeConfig;
    }

    public RunnerExecutionConfig execution() {
        return executionConfig;
    }

    // Transitional getters for existing callers
    public String getRunnerToken() {
        return runnerToken();
    }

    public String getMasterBaseUrl() {
        return serverAuthority();
    }

    public java.time.Duration getPollInterval() {
        return runtimeConfig != null ? runtimeConfig.getPollInterval() : null;
    }

    public java.time.Duration getBusyWaitInterval() {
        return runtimeConfig != null ? runtimeConfig.getBusyWaitInterval() : null;
    }

    public String getGrpcHost() {
        return grpcHost();
    }

    public Integer getGrpcPort() {
        return grpcPort();
    }

    public String getRunnerImageName() {
        return executionConfig != null ? executionConfig.getRunnerImageName() : null;
    }

    public String getJenkinsPluginConfig() {
        return executionConfig != null ? executionConfig.getJenkinsPluginConfig() : null;
    }
}
