package io.jgitkins.server.infrastructure.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jgitkins.security.jwt")
public class JwtProperties {

    private String secret;
    private long ttlSeconds;
}
