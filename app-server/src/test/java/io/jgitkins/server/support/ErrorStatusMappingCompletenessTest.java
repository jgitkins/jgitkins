package io.jgitkins.server.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.common.presentation.advice.mapper.ErrorHttpStatusMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * {@link ErrorStatusMappingTestConfig} must carry every {@link ErrorHttpStatusMapper} that exists.
 *
 * <p>This guard exists because the list already drifted twice before it was centralized:
 * {@code RunnerControllerTest} and {@code RepositoryContentControllerTest} each hand-built a composite
 * without {@code PresentationErrorHttpStatusMapper}. A {@code PresentationErrorCode} in either context
 * would have fallen to {@code CompositeErrorHttpStatusMapper}'s {@code orElse(INTERNAL_SERVER_ERROR)}
 * while production answered 400 or 401, and no test would have failed, because neither context throws
 * one today. The drift was invisible precisely because it was latent.
 *
 * <p>Asserted against the source tree rather than a classpath scan so the failure names the missing
 * class. A new mapper added to production and forgotten here degrades that whole error family to 500
 * in every test that uses this config.
 */
class ErrorStatusMappingCompletenessTest {

    @Test
    void theTestConfigCarriesEveryMapperThatExists() throws IOException {
        Path mapperDir = Path.of("app-server/src/main/java/io/jgitkins/server/common/presentation/advice/mapper");
        if (!Files.isDirectory(mapperDir)) {
            mapperDir = Path.of("src/main/java/io/jgitkins/server/common/presentation/advice/mapper");
        }

        Set<String> onDisk = new TreeSet<>();
        try (Stream<Path> files = Files.list(mapperDir)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    if (Files.readString(path).contains("implements ErrorHttpStatusMapper")) {
                        onDisk.add(path.getFileName().toString().replace(".java", ""));
                    }
                } catch (IOException cannotRead) {
                    throw new IllegalStateException("unreadable mapper source: " + path, cannotRead);
                }
            });
        }

        List<ErrorHttpStatusMapper> configured = ErrorStatusMappingTestConfig.delegates();
        Set<String> imported = new TreeSet<>();
        configured.forEach(mapper -> imported.add(mapper.getClass().getSimpleName()));

        assertThat(imported)
                .as("ErrorStatusMappingTestConfig.realMapper() must carry every ErrorHttpStatusMapper "
                        + "implementation. A missing one silently degrades that error family to 500 in "
                        + "every test that uses this config, and nothing fails until production throws it.")
                .isEqualTo(onDisk);
    }
}
