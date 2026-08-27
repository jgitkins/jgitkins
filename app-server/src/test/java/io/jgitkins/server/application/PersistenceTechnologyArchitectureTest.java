package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps JPA inside the outbound persistence adapter as the migration spreads.
 *
 * <p>Task 2.66 forbids {@code jakarta.persistence} under the domain and application roots. Until
 * Task 2.69 there was no JPA on the classpath, so the rule cost nothing to obey. Now there is, and
 * Tasks 2.70-2.77 add an entity set per bounded context. The cheapest way for that to go wrong is an
 * entity or a {@code LockModeType} drifting into a port signature or a domain type, at which point
 * the persistence provider becomes visible to business code and the selector can no longer swap it.
 *
 * <p>Scope is deliberately the whole of {@code app-server} main source rather than one context: the
 * point is to catch the first context that gets it wrong, whichever one that is.
 */
class PersistenceTechnologyArchitectureTest {

    private static final Path MAIN_ROOT = resolveRoot();

    /** Package fragments where persistence technology is allowed to appear. */
    private static final List<String> ALLOWED_FRAGMENTS = List.of(
            "/adapter/out/persistence/",
            "/infrastructure/persistence/",
            "/common/infrastructure/config/");

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import jakarta.persistence.",
            "import org.hibernate.",
            "import org.springframework.data.jpa.",
            "import org.springframework.orm.jpa.");

    private static Path resolveRoot() {
        Path local = Path.of("src/main/java/io/jgitkins/server");
        return Files.isDirectory(local) ? local : Path.of("app-server/src/main/java/io/jgitkins/server");
    }

    @Test
    void jpaTypesStayInsidePersistenceAdapter() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(MAIN_ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relative = MAIN_ROOT.relativize(path).toString().replace('\\', '/');
                if (ALLOWED_FRAGMENTS.stream().anyMatch(("/" + relative)::contains)) {
                    continue;
                }
                for (String line : Files.readAllLines(path)) {
                    String trimmed = line.trim();
                    FORBIDDEN_IMPORT_PREFIXES.stream()
                            .filter(trimmed::startsWith)
                            .findFirst()
                            .ifPresent(prefix -> violations.add(relative + " -> " + trimmed));
                }
            }
        }
        assertThat(violations)
                .as("persistence technology must stay inside the outbound persistence adapter; once a "
                        + "JPA type reaches a port or a domain class, business code depends on the "
                        + "provider and the persistence selector can no longer swap it")
                .isEmpty();
    }

    /**
     * The guard is only worth having if the allowlist actually names where JPA lives today. If the
     * reference slice moved and nobody updated this test, the check above would pass by scanning
     * nothing relevant.
     */
    @Test
    void theAllowedLocationActuallyContainsTheJpaReferenceSlice() {
        Path jpaSlice = MAIN_ROOT.resolve("collaboration/adapter/out/persistence/jpa");
        assertThat(Files.isDirectory(jpaSlice))
                .as("the JPA reference slice is expected under an allowlisted package; if it moved, "
                        + "update ALLOWED_FRAGMENTS instead of letting this guard scan nothing")
                .isTrue();
    }
}
