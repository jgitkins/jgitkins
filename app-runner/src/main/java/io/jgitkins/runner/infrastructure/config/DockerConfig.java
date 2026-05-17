package io.jgitkins.runner.infrastructure.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(RunnerDockerProperties.class)
public class DockerConfig {

    @Bean
    public DockerClient dockerClient(RunnerDockerProperties properties) {

        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig
                .createDefaultConfigBuilder();

        if (StringUtils.hasText(properties.getHost())) {
            builder.withDockerHost(properties.getHost());
        }
        if (properties.getTlsVerify() != null) {
            builder.withDockerTlsVerify(properties.getTlsVerify());
        }
        if (StringUtils.hasText(properties.getCertPath())) {
            builder.withDockerCertPath(properties.getCertPath());
        }

        DefaultDockerClientConfig config = builder.build();

        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        log.info("Docker host resolved to {}", config.getDockerHost());

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
