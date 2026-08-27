package io.jgitkins.server.persistence.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.67 placement gate: persistence technology types live under the owning context's outbound
 * persistence adapter, and nowhere else.
 *
 * <p>The boundary this pins is the one {@code docs/architecture/persistence-boundary.md} argues for.
 * Its value is not tidiness. While persistence models sat under {@code infrastructure}, any application
 * or domain class could import one without crossing a package the guards were watching, and the leak
 * would look like an ordinary import. Concentrating them under {@code adapter/out/persistence} is what
 * lets a single rule — nothing outside the adapter may name these types — be checkable at all.
 *
 * <p>Both directions are asserted. The first test says the types are at the destination; the second says
 * the old location is gone rather than merely also populated, because a half-move leaves two homes and
 * the next author picks whichever they saw first.
 */
class PersistenceModelPlacementArchitectureTest {

    private static final Path SERVER_ROOT = Path.of("src/main/java/io/jgitkins/server");

    private static final List<String> CONTEXTS =
            List.of("change/review", "collaboration", "execution", "identity/access", "repository");

    /** The three leaves task 2.67 moved, as {@code source leaf -> destination leaf}. */
    private static final Map<String, String> MOVED_LEAVES = new LinkedHashMap<>(Map.of(
            "infrastructure/persistence/model", "adapter/out/persistence/model",
            "infrastructure/persistence/mapper", "adapter/out/persistence/mapper",
            "infrastructure/mapper", "adapter/out/persistence/support"));

    @Test
    void persistenceModelsResideOnlyUnderAdapterOutPersistence() throws IOException {
        Map<String, Integer> populatedDestinations = new LinkedHashMap<>();

        for (String context : CONTEXTS) {
            for (String destination : MOVED_LEAVES.values()) {
                Path leaf = SERVER_ROOT.resolve(context).resolve(destination);
                if (Files.isDirectory(leaf)) {
                    populatedDestinations.put(context + "/" + destination, javaFileCount(leaf));
                }
            }
        }

        assertThat(populatedDestinations)
                .as("no context has any of the three destination leaves; the move did not happen, or "
                        + "this test is looking in the wrong place")
                .isNotEmpty();

        assertThat(populatedDestinations.values())
                .as("a destination leaf that exists but holds no .java file is a directory left behind "
                        + "by a partial move: %s", populatedDestinations)
                .allSatisfy(count -> assertThat(count).isPositive());

        // Every mapper interface and model must be inside an adapter/out/persistence package. Checked by
        // package declaration rather than by path, because the two can disagree and it is the package
        // that decides what a caller writes in an import.
        List<String> misplaced = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SERVER_ROOT)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                String name = file.getFileName().toString();
                boolean isPersistenceType = name.endsWith("MbgMapper.java")
                        || name.endsWith("Entity.java")
                        || name.endsWith("EntityCondition.java")
                        || name.endsWith("DomainMapper.java");
                if (!isPersistenceType) {
                    continue;
                }
                String source = Files.readString(file);
                if (!source.contains("package io.jgitkins.server.")) {
                    continue;
                }
                String declared = source.substring(source.indexOf("package "), source.indexOf(';'));
                if (!declared.contains(".adapter.out.persistence")) {
                    misplaced.add(file + " declares " + declared.trim());
                }
            }
        }

        assertThat(misplaced)
                .as("these persistence technology types are not in an adapter.out.persistence package. "
                        + "Outside the adapter, an application or domain class can import one without "
                        + "crossing any package a guard watches, and the leak reads as an ordinary import.")
                .isEmpty();
    }

    @Test
    void infrastructurePersistencePackagesAreEmptyAfterPlacement() throws IOException {
        Map<String, List<String>> leftBehind = new LinkedHashMap<>();

        for (String context : CONTEXTS) {
            for (String source : MOVED_LEAVES.keySet()) {
                Path leaf = SERVER_ROOT.resolve(context).resolve(source);
                if (!Files.isDirectory(leaf)) {
                    continue;
                }
                List<String> remaining;
                try (Stream<Path> files = Files.walk(leaf)) {
                    remaining = files.filter(Files::isRegularFile)
                            .map(Path::toString)
                            .sorted()
                            .toList();
                }
                if (!remaining.isEmpty()) {
                    leftBehind.put(context + "/" + source, remaining);
                }
            }
        }

        assertThat(leftBehind)
                .as("these legacy persistence packages still hold files. A half-move is worse than no "
                        + "move: there are now two homes for the same kind of type, and the next author "
                        + "adds to whichever they saw first.")
                .isEmpty();
    }

    @Test
    void applicationAndDomainDoNotImportTheRelocatedPersistencePackages() throws IOException {
        // Concentrating the types under adapter/out/persistence is only useful if something asserts the
        // concentration holds. Before task 2.67 the existing guards forbade application and domain from
        // importing <context>/infrastructure — which, after the move, no longer covers these types at
        // all. Without this rule the relocation would have *weakened* the boundary while appearing to
        // strengthen it.
        List<String> violations = new ArrayList<>();

        for (String context : CONTEXTS) {
            for (String layer : List.of("application", "domain")) {
                Path root = SERVER_ROOT.resolve(context).resolve(layer);
                if (!Files.isDirectory(root)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .sorted()
                            .toList()) {
                        for (String line : Files.readAllLines(file)) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("import io.jgitkins.server.")
                                    && trimmed.contains(".adapter.out.persistence.")) {
                                violations.add(file + " -> " + trimmed);
                            }
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .as("application and domain must not name a type from the outbound persistence adapter. "
                        + "Any one of these imports makes business code depend on a persistence "
                        + "technology, and the selector stops being able to swap providers without "
                        + "touching that code.")
                .isEmpty();
    }

    private static int javaFileCount(Path leaf) throws IOException {
        try (Stream<Path> files = Files.walk(leaf)) {
            return (int) files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();
        }
    }
}
