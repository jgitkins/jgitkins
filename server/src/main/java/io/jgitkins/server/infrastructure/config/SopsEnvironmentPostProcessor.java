package io.jgitkins.server.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SopsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SECRETS_DIR_PROPERTY = "jgitkins.secrets.dir";
    private static final String SECRETS_DIR_ENV = "JGITKINS_SECRETS_DIR";
    private static final String MODULE_DIR = "server";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        String profile = Optional.ofNullable(
                environment.getProperty("spring.profiles.active")).orElse("local");

        Path encPath = resolveEncryptedSecretPath(profile, environment, currentWorkingDirectory());
        if (encPath == null) {
            // local/dev runs should not fail when secrets are missing
            return;
        }

        if (!isSopsAvailable()) {
            // CI or fresh dev environments may not have sops installed.
            return;
        }

        try {
            Process process = new ProcessBuilder(
                    "sops", "-d", encPath.toString()).redirectErrorStream(true).start();

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

    private boolean isSopsAvailable() {
        try {
            Process process = new ProcessBuilder("sops", "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    Path resolveEncryptedSecretPath(
            String profile,
            ConfigurableEnvironment environment,
            Path workingDirectory
    ) {
        String secretFileName = "app." + profile + ".enc.yaml";
        String configuredSecretsDir = Optional.ofNullable(environment.getProperty(SECRETS_DIR_PROPERTY))
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(System.getProperty(SECRETS_DIR_PROPERTY))
                        .filter(value -> !value.isBlank()))
                .or(() -> Optional.ofNullable(System.getenv(SECRETS_DIR_ENV))
                        .filter(value -> !value.isBlank()))
                .orElse(null);

        List<Path> candidates = configuredSecretsDir == null
                ? List.of(
                workingDirectory.resolve("secrets").resolve(secretFileName),
                workingDirectory.resolve(MODULE_DIR).resolve("secrets").resolve(secretFileName)
        )
                : List.of(Paths.get(configuredSecretsDir).resolve(secretFileName));

        return candidates.stream()
                .map(Path::normalize)
                .filter(path -> path.toFile().exists())
                .findFirst()
                .orElse(null);
    }

    private Path currentWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

}
