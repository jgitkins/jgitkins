package io.jgitkins.server.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Keeps JPA inside the outbound persistence adapter.
 *
 * <p>Task 2.66 forbids {@code jakarta.persistence} under the domain and application roots. Until
 * Task 2.69 there was no JPA on the classpath, so the rule cost nothing to obey; tasks 2.70-2.77
 * added an entity set per bounded context, and the cheapest way for that to go wrong is an entity or
 * a {@code LockModeType} drifting into a port signature or a domain type.
 *
 * <p>The reason has outlived the selector it was written for. It used to be that a leak made the
 * provider visible to business code and the selector could no longer swap it; there is one provider
 * now and nothing to swap. What a leak costs today is the ability to change how a table is read
 * without changing the code that asked -- and the next provider change, whenever it comes, starts
 * from wherever these imports have reached by then.
 *
 * <p>Scope is deliberately the whole of {@code app-server} main source rather than one context: the
 * point is to catch the first context that gets it wrong, whichever one that is.
 */
class PersistenceTechnologyArchitectureTest {

    private static final Path MAIN_ROOT = resolveRoot();

    /**
     * Package fragments where persistence technology is allowed to appear.
     *
     * <p>{@code /infrastructure/persistence/} was removed in task 2.67: the 53 files that lived there
     * moved into the outbound adapter, and leaving the allowance behind would have silently permitted a
     * persistence type to reappear at the old address. A dead allowance is worse than no allowance,
     * because it reads as a deliberate exception.
     */
    private static final List<String> ALLOWED_FRAGMENTS = List.of(
            "/adapter/out/persistence/",
            "/common/infrastructure/config/");

    /**
     * Both provider families, not just JPA.
     *
     * <p>Task 2.67 added the MyBatis rows. A guard that forbids only {@code jakarta.persistence} leaves
     * {@code org.apache.ibatis} free to reappear in {@code application} and {@code domain} — the same
     * leak, through the other provider, and the one that was actually there before the migration
     * started. Naming one technology is not a boundary.
     */
    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import jakarta.persistence.",
            "import org.hibernate.",
            "import org.springframework.data.jpa.",
            "import org.springframework.orm.jpa.",
            "import org.apache.ibatis.",
            "import org.mybatis.");

    /**
     * Files allowed to name a persistence technology despite being outside the adapter, each with why.
     *
     * <p>Kept as a file allowlist rather than a widened package fragment. A fragment would exempt
     * everything at that path forever; a named file exempts one class and makes the next addition a
     * visible decision.
     */
    private static final Map<String, String> ALLOWED_FILES = Map.of(
            "JGitkinsServerApplication.java",
            "the composition root declares @MapperScan, which is what registers the mapper beans the "
                    + "adapters inject. The alternative is moving the scan into a configuration class "
                    + "purely to satisfy this test, which would hide the application's own wiring.");

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
                if (ALLOWED_FILES.containsKey(path.getFileName().toString())) {
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
                        + "provider and cannot be read without knowing which one")
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
