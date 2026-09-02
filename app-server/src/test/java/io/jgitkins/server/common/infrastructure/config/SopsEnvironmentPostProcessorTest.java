package io.jgitkins.server.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class SopsEnvironmentPostProcessorTest {

    private final SopsEnvironmentPostProcessor processor = new SopsEnvironmentPostProcessor();

    @TempDir
    Path tempDir;

    @Test
    void resolveEncryptedSecretPath_findsModuleSecretWhenRunningFromMonorepoRoot() throws IOException {
        Path expected = createFile(tempDir.resolve("server/secrets/app.local.enc.yaml"));

        Path resolved = resolveEncryptedSecretPath("local", new MockEnvironment(), tempDir);

        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    void resolveEncryptedSecretPath_prefersConfiguredSecretDirectory() throws IOException {
        Path customSecretsDir = Files.createDirectories(tempDir.resolve("custom-secrets"));
        Path expected = createFile(customSecretsDir.resolve("app.local.enc.yaml"));

        MockEnvironment environment = new MockEnvironment()
                .withProperty("jgitkins.secrets.dir", customSecretsDir.toString());

        Path resolved = resolveEncryptedSecretPath("local", environment, tempDir);

        assertThat(resolved).isEqualTo(expected);
    }

    private Path resolveEncryptedSecretPath(String profile, MockEnvironment environment, Path rootDir) {
        try {
            Method method = SopsEnvironmentPostProcessor.class.getDeclaredMethod(
                    "resolveEncryptedSecretPath",
                    String.class,
                    org.springframework.core.env.ConfigurableEnvironment.class,
                    Path.class);
            method.setAccessible(true);
            return (Path) method.invoke(processor, profile, environment, rootDir);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to invoke resolveEncryptedSecretPath", ex);
        }
    }

    private Path createFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "REST_PORT: 8080\n");
    }
}
