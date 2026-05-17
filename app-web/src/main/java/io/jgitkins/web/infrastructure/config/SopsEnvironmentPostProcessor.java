package io.jgitkins.web.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeUnit;

@Slf4j
public class SopsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final long SOPS_TIMEOUT_SECONDS = 15;
    private static final String SECRETS_DIR_PROPERTY = "jgitkins.secrets.dir";
    private static final String SECRETS_DIR_ENV = "JGITKINS_SECRETS_DIR";
    private static final String MODULE_DIR = "web";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {
        String profile = Optional.ofNullable(
                environment.getProperty("spring.profiles.active")
        ).orElse("local");

        log.debug("Loading SOPS secrets for profile={}", profile);
        Path encPath = resolveEncryptedSecretPath(profile, environment, currentWorkingDirectory());
        if (encPath == null) {
            // local/dev runs should not fail when secrets are missing
            log.debug("SOPS secret file not found for profile={}, cwd={}", profile, currentWorkingDirectory());
            return;
        }

        if (!isSopsAvailable()) {
            log.warn("sops binary is not available, skipping optional secret file: {}", encPath);
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

    private String decryptWithSops(Path encPath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("sops", "-d", encPath.toString())
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

    private boolean isSopsAvailable() {
        try {
            Process process = new ProcessBuilder("sops", "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(SOPS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    && process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
