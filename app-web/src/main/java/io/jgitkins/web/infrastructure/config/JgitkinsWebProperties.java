package io.jgitkins.web.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jgitkins.web")
public record JgitkinsWebProperties(String prUrlTemplate, String issueUrlTemplate) {
}
