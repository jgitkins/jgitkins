package io.jgitkins.runner.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "runner.docker")
public class RunnerDockerProperties {
    private String host;
    private Boolean tlsVerify;
    private String certPath;
}
