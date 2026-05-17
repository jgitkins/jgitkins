package io.jgitkins.runner.infrastructure.mapper;

import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.domain.RunnerExecutionConfig;
import io.jgitkins.runner.domain.RunnerRuntimeConfig;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerEntity;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RunnerDomainMapper {

    String KEY_REST_HOST = "restHost";
    String KEY_REST_PORT = "restPort";
    String KEY_REST_BASE_PATH = "restBasePath";
    String KEY_GRPC_HOST = "grpcHost";
    String KEY_GRPC_PORT = "grpcPort";
    String KEY_POLL_INTERVAL = "pollIntervalMillis";
    String KEY_BUSY_INTERVAL = "busyWaitIntervalMillis";
    String KEY_RUNNER_IMAGE = "runnerImageName";
    String KEY_JENKINS_PLUGIN = "jenkinsPluginConfig";

    default RunnerConfiguration toDomain(RunnerEntity runner, Map<String, String> configMap) {
        RunnerRuntimeConfig runtime = RunnerRuntimeConfig.builder()
                                                         .runnerToken(runner.getToken())
                                                         .restHost(configMap.get(KEY_REST_HOST))
                                                         .restPort(parseInt(configMap.get(KEY_REST_PORT)))
                                                         .restBasePath(configMap.get(KEY_REST_BASE_PATH))
                                                         .grpcHost(configMap.get(KEY_GRPC_HOST))
                                                         .grpcPort(parseInt(configMap.get(KEY_GRPC_PORT)))
                                                         .pollInterval(toDuration(configMap.get(KEY_POLL_INTERVAL)))
                                                         .busyWaitInterval(toDuration(configMap.get(KEY_BUSY_INTERVAL)))
                                                         .build();
        RunnerExecutionConfig execution = RunnerExecutionConfig.builder()
                                                               .runnerImageName(configMap.get(KEY_RUNNER_IMAGE))
                                                               .jenkinsPluginConfig(configMap.get(KEY_JENKINS_PLUGIN))
                                                               .build();
        return RunnerConfiguration.builder()
                                   .runtimeConfig(runtime)
                                   .executionConfig(execution)
                                   .build();
    }

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", constant = "")
    @Mapping(target = "token", source = "runnerToken")
    RunnerEntity toEntity(RunnerConfiguration source);

    default Map<String, String> toRuntimeConfigMap(RunnerConfiguration source) {
        Map<String, String> map = new HashMap<>();
        putIfNotNull(map, KEY_REST_HOST, source.restHost());
        putIfNotNull(map, KEY_REST_PORT, source.restPort() == null ? null : Integer.toString(source.restPort()));
        putIfNotNull(map, KEY_REST_BASE_PATH, source.restBasePath());
        putIfNotNull(map, KEY_GRPC_HOST, source.grpcHost());
        putIfNotNull(map, KEY_GRPC_PORT, source.grpcPort() == null ? null : Integer.toString(source.grpcPort()));
        Duration poll = source.getPollInterval();
        if (poll != null) {
            map.put(KEY_POLL_INTERVAL, Long.toString(poll.toMillis()));
        }
        Duration busy = source.getBusyWaitInterval();
        if (busy != null) {
            map.put(KEY_BUSY_INTERVAL, Long.toString(busy.toMillis()));
        }
        return map;
    }

    default Map<String, String> toExecutionConfigFileMap(RunnerConfiguration source) {
        Map<String, String> map = new HashMap<>();
        putIfNotNull(map, KEY_RUNNER_IMAGE, source.getRunnerImageName());
        putIfNotNull(map, KEY_JENKINS_PLUGIN, source.getJenkinsPluginConfig());
        return map;
    }

    default Duration toDuration(String millis) {
        if (millis == null || millis.isBlank()) {
            return Duration.ZERO;
        }
        try {
            return Duration.ofMillis(Long.parseLong(millis));
        } catch (NumberFormatException ex) {
            return Duration.ZERO;
        }
    }

    default void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    default Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
