package io.jgitkins.runner.application.mapper;

import io.jgitkins.runner.application.dto.RunnerExecutionConfigResult;
import io.jgitkins.runner.application.dto.RunnerRuntimeConfigResult;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.domain.RunnerExecutionConfig;
import io.jgitkins.runner.domain.RunnerRuntimeConfig;
import io.jgitkins.runner.infrastructure.http.ActivationResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RunnerConfigurationMapper {

    public RunnerConfiguration toDomain(ActivationResult response, String runnerToken, String overrideBaseUrl) {
        RunnerRuntimeConfig runtime = toRuntimeConfig(response.runtimeConfig(), runnerToken, overrideBaseUrl);
        RunnerExecutionConfig execution = toExecutionConfig(response.executionConfig());
        return RunnerConfiguration.builder()
                                  .runtimeConfig(runtime)
                                  .executionConfig(execution)
                                  .build();
    }

    private RunnerRuntimeConfig toRuntimeConfig(RunnerRuntimeConfigResult result, String runnerToken, String overrideBaseUrl) {
        if (result == null) {
            result = RunnerRuntimeConfigResult.builder().build();
        }
        HostPort override = parseHostPort(overrideBaseUrl);
        String restHost = firstNonBlank(override.host, result.getRestHost());
        Integer restPort = override.port != null ? override.port : result.getRestPort();

        return RunnerRuntimeConfig.builder()
                                  .runnerToken(runnerToken)
                                  .restHost(restHost)
                                  .restPort(restPort)
                                  .restBasePath(result.getRestBasePath())
                                  .grpcHost(result.getGrpcHost())
                                  .grpcPort(result.getGrpcPort())
                                  .pollInterval(toDuration(result.getPollIntervalMs()))
                                  .busyWaitInterval(toDuration(result.getBusyWaitIntervalMs()))
                                  .build();
    }

    private RunnerExecutionConfig toExecutionConfig(RunnerExecutionConfigResult result) {
        if (result == null) {
            return null;
        }
        return RunnerExecutionConfig.builder()
                                    .runnerImageName(result.getRunnerImageName())
                                    .jenkinsPluginConfig(result.getJenkinsPluginConfig())
                                    .build();
    }

    private Duration toDuration(Long millis) {
        if (millis == null || millis <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(millis);
    }

    private String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return (secondary != null && !secondary.isBlank()) ? secondary : null;
    }

    private HostPort parseHostPort(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return HostPort.none();
        }
        try {
            URI uri = new URI(baseUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null && !baseUrl.startsWith("http")) {
                // try host:port without scheme
                String[] parts = baseUrl.split(":");
                host = parts.length > 0 ? parts[0] : null;
                port = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
            }
            return new HostPort(host, port > 0 ? port : null);
        } catch (URISyntaxException | NumberFormatException ex) {
            return HostPort.none();
        }
    }

    private record HostPort(String host, Integer port) {
        private static HostPort none() {
            return new HostPort(null, null);
        }
    }
}
