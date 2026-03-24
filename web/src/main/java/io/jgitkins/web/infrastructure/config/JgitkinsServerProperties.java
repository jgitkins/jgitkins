package io.jgitkins.web.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "jgitkins.server")
public record JgitkinsServerProperties(URI baseUrl) {
}
