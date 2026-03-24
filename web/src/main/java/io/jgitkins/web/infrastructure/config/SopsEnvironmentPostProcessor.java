package io.jgitkins.web.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SopsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final long SOPS_TIMEOUT_SECONDS = 15;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        String profile = Optional.ofNullable(
                environment.getProperty("spring.profiles.active")
        ).orElse("local");

        log.debug("Loading SOPS secrets for profile={}", profile);
        String encPath = "secrets/app." + profile + ".enc.yaml";
        File encFile = new File(encPath);

        if (!encFile.exists()) {
            // local/dev runs should not fail when secrets are missing
            log.debug("SOPS secret file not found, skipping: {}", encPath);
            return;
        }

        try {
            String decrypted = decryptWithSops(encPath);

            Object loaded = new Yaml().load(decrypted);
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException("SOPS YAML must be a map");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) loaded;

            environment.getPropertySources().addFirst(
                    new MapPropertySource("sops:" + encPath, values)
            );
            log.info("Loaded SOPS secrets: {}", encPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading SOPS secrets: " + encPath, e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load SOPS secrets: " + encPath, e
            );
        }
    }

    @Override
    public int getOrder() {
        // Before application.yml
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String decryptWithSops(String encPath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("sops", "-d", encPath)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(SOPS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String output;
        try (InputStream is = process.getInputStream()) {
            output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("sops decrypt timed out after " + SOPS_TIMEOUT_SECONDS + "s");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("sops decrypt failed: " + output);
        }
        return output;
    }
}
