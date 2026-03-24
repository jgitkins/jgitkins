package io.jgitkins.server.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class SopsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        String profile = Optional.ofNullable(
                environment.getProperty("spring.profiles.active")).orElse("local");

        String encPath = "secrets/app." + profile + ".enc.yaml";
        File encFile = new File(encPath);

        if (!encFile.exists()) {
            // local/dev runs should not fail when secrets are missing
            return;
        }

        try {
            Process process = new ProcessBuilder(
                    "sops", "-d", encPath).redirectErrorStream(true).start();

            String decrypted;
            try (InputStream is = process.getInputStream()) {
                decrypted = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("sops decrypt failed");
            }

            Object loaded = new Yaml().load(decrypted);
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException("SOPS YAML must be a map");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) loaded;

            environment.getPropertySources().addFirst(
                    new MapPropertySource("sops:" + encPath, values));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load SOPS secrets: " + encPath, e);
        }
    }

    @Override
    public int getOrder() {
        // Before application.yml
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
