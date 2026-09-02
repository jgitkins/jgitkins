package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No two bounded contexts may declare a type with the same simple name.
 *
 * <p>An unqualified import silently picks one of them, and the compiler is happy either way. Task
 * 2.132 filed five such collisions; three were real by the time this guard was written, and each had
 * already cost something:
 *
 * <ul>
 *   <li>{@code OwnerId} meant "repository owner" in shared and "organization owner" in
 *       collaboration, and the two threw different exceptions -- so the same mistake answered 400 in
 *       one context and 500 in the other.
 *   <li>{@code DomainEventPublisher} existed twice; one was dead and nobody could tell which by
 *       reading an import.
 *   <li>{@code OrganizeAlreadyExistsException} named two different questions -- creating an
 *       organization, and activating a user whose username is taken as a namespace.
 * </ul>
 *
 * <p>The cost is not only ambiguity at the import. A test named {@code IdentifierInvariantTest},
 * living in {@code shared.domain}, imported collaboration's {@code OwnerId} and therefore never
 * covered the shared type eleven repository files depend on -- while looking exactly like the test
 * that did.
 *
 * <p>Scoped to {@code src/main}. Test sources may legitimately repeat a name across contexts
 * (per-context {@code ...ControllerTest}), and a test picking the wrong production type fails
 * loudly rather than shipping.
 */
class CrossContextSimpleNameCollisionTest {

    /**
     * The top-level packages under {@code io.jgitkins.server} that count as separate contexts.
     * {@code shared} and {@code common} are included on purpose: a name that exists both in the
     * shared kernel and in one context is the most confusing case of all, because the shared one
     * looks like the canonical answer. That is exactly what {@code OwnerId} was.
     */
    private static final List<String> CONTEXTS = List.of(
            "collaboration", "change/review", "identity/access", "repository", "execution",
            "shared", "common");

    /**
     * File names that declare no type, so two contexts holding one is not a collision. There is
     * nothing for an unqualified import to pick between: {@code package-info} carries package
     * annotations and javadoc, {@code module-info} the module declaration, and neither is nameable
     * from another file. Before the first {@code package-info.java} landed this repo had none, so
     * the rule had never met the case and reported all three contexts as colliding on the name.
     */
    private static final List<String> DECLARES_NO_TYPE = List.of("package-info", "module-info");

    @Test
    void noTwoContextsDeclareTheSameSimpleName() throws IOException {
        List<Path> sources = mainSources();

        assertThat(sources)
                .as("the walk found no sources, which means it did not find the source tree -- an "
                        + "empty input makes every assertion below vacuously true")
                .hasSizeGreaterThan(100);

        Map<String, List<String>> collisions = collisionsIn(ArchitectureScanner.mainRoot(), sources);

        assertThat(collisions)
                .as("a type name that exists in two contexts makes an unqualified import pick one of "
                        + "them silently, and the compiler accepts either. Rename one of them for the "
                        + "concept it actually names -- do not merge them until you have checked they "
                        + "mean the same thing.")
                .isEmpty();
    }

    @Test
    void theCheckActuallyFiresOnACollision() {
        Path root = Path.of("io/jgitkins/server");
        List<Path> synthetic = List.of(
                root.resolve("repository/domain/vo/OwnerId.java"),
                root.resolve("collaboration/domain/vo/OwnerId.java"),
                root.resolve("execution/domain/vo/RunnerId.java"));

        Map<String, List<String>> collisions = collisionsIn(root, synthetic);

        assertThat(collisions)
                .as("without this the guard above passes whenever the walk returns nothing, and "
                        + "nobody finds out until two contexts have already grown the same name")
                .containsOnlyKeys("OwnerId");
        assertThat(collisions.get("OwnerId")).containsExactly("collaboration", "repository");
    }

    /**
     * The exclusion is narrow on purpose. It must swallow {@code package-info} in every context and
     * nothing else, so this feeds the same call both at once: three package-infos that must not
     * collide, and a real type beside them that must.
     */
    @Test
    void filesDeclaringNoTypeAreNotCollisions_butRealTypesBesideThemStillAre() {
        Path root = Path.of("io/jgitkins/server");
        List<Path> synthetic = List.of(
                root.resolve("execution/application/internal/package-info.java"),
                root.resolve("identity/access/application/internal/package-info.java"),
                root.resolve("repository/application/internal/package-info.java"),
                root.resolve("execution/application/internal/JobPlan.java"),
                root.resolve("repository/application/internal/JobPlan.java"));

        Map<String, List<String>> collisions = collisionsIn(root, synthetic);

        assertThat(collisions)
                .as("package-info declares no type, so no import can pick between two of them; a "
                        + "real name in the same input must still be caught, or the exclusion is "
                        + "silently swallowing more than it should")
                .containsOnlyKeys("JobPlan");
        assertThat(collisions.get("JobPlan")).containsExactly("execution", "repository");
    }

    private static List<Path> mainSources() throws IOException {
        Path root = ArchitectureScanner.mainRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    /** Pure over its inputs, so the negative control can hand it a collision that is not on disk. */
    private static Map<String, List<String>> collisionsIn(Path root, List<Path> sources) {
        Map<String, TreeSet<String>> byName = new LinkedHashMap<>();
        for (Path source : sources) {
            String relative = root.relativize(source).toString();
            contextOf(relative).ifPresent(context -> {
                String simpleName = source.getFileName().toString().replace(".java", "");
                if (DECLARES_NO_TYPE.contains(simpleName)) {
                    return;
                }
                byName.computeIfAbsent(simpleName, key -> new TreeSet<>()).add(context);
            });
        }
        Map<String, List<String>> collisions = new TreeMap<>();
        byName.forEach((simpleName, contexts) -> {
            if (contexts.size() > 1) {
                collisions.put(simpleName, new ArrayList<>(contexts));
            }
        });
        return collisions;
    }

    private static java.util.Optional<String> contextOf(String relativePath) {
        return CONTEXTS.stream().filter(context -> relativePath.startsWith(context + "/")).findFirst();
    }
}
