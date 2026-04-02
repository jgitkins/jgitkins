package io.jgitkins.web.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SopsEnvironmentPostProcessorTest {

    private final SopsEnvironmentPostProcessor processor = new SopsEnvironmentPostProcessor();

    @TempDir
    Path tempDir;

    @Test
    void resolveEncryptedSecretPath_findsModuleSecretWhenRunningFromMonorepoRoot() throws IOException {
        Path expected = createFile(tempDir.resolve("web/secrets/app.local.enc.yaml"));

        Path resolved = processor.resolveEncryptedSecretPath("local", new MockEnvironment(), tempDir);

        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    void resolveEncryptedSecretPath_prefersConfiguredSecretDirectory() throws IOException {
        Path customSecretsDir = Files.createDirectories(tempDir.resolve("custom-secrets"));
        Path expected = createFile(customSecretsDir.resolve("app.local.enc.yaml"));

        MockEnvironment environment = new MockEnvironment()
                .withProperty("jgitkins.secrets.dir", customSecretsDir.toString());

        Path resolved = processor.resolveEncryptedSecretPath("local", environment, tempDir);

        assertThat(resolved).isEqualTo(expected);
    }

    private Path createFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, "REST_PORT: 8080\n");
    }
}
