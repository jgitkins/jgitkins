package io.jgitkins.server.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties("jgitkins.server.runtime")
public class RunnerRuntimeProperties {

    @NotBlank
    private String serviceHost;

    @NotBlank
    private String restScheme;

    private int restPort;

    @NotBlank
    private String restBasePath = "/api";

    private int grpcPort;

    // TODO
    private long pollIntervalMs = 5000L;

    private long busyWaitIntervalMs = 1000L;
}
