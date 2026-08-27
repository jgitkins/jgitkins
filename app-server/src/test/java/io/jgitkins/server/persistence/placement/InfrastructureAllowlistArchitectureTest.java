package io.jgitkins.server.persistence.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.67 ownership gate: after the move, {@code <context>/infrastructure} keeps configuration and
 * nothing else.
 *
 * <p>Stating what {@code infrastructure} is *for* is the half of this task that outlives the move. A
 * directory with no rule accumulates whatever does not obviously belong elsewhere, which is how the
 * persistence models ended up there to begin with. The rule is: composition roots, Spring
 * configuration, properties classes, and module-local support that is genuinely infrastructural — not
 * persistence types, not domain mappers, not adapters.
 *
 * <p>The allowlist is per-leaf and each entry carries its reason. Two entries are exceptions rather
 * than examples of the rule, and both are recorded as such rather than quietly widening it.
 */
class InfrastructureAllowlistArchitectureTest {

    private static final Path SERVER_ROOT = Path.of("src/main/java/io/jgitkins/server");

    private static final List<String> BOUNDED_CONTEXTS =
            List.of("change/review", "collaboration", "execution", "identity/access", "repository");

    /** Leaf directories a bounded context may keep under {@code infrastructure}, with the reason. */
    private static final Map<String, String> ALLOWED_LEAVES = new LinkedHashMap<>(Map.of(
            "config",
            "composition roots and Spring configuration -- this is what infrastructure is for",
            "support",
            "EXCEPTION, repository context only: RepositoryFileSystemHelper and RepositoryResolver are "
                    + "filesystem and git-path helpers, not persistence. They are infrastructural but "
                    + "are not configuration, so the rule as written does not cover them. Moving them "
                    + "is a separate decision about where filesystem access belongs and is deliberately "
                    + "not made by this task"));

    /** Types that must never appear under {@code infrastructure} again, with what each one signals. */
    private static final Map<String, String> FORBIDDEN_SUFFIXES = new LinkedHashMap<>(Map.of(
            "MbgMapper.java", "a MyBatis mapper interface belongs to the outbound persistence adapter",
            "EntityCondition.java", "an MBG condition object is a persistence technology type",
            "DomainMapper.java",
            "a domain mapper translates between persistence model and aggregate, which is adapter work",
            "JpaEntity.java", "a JPA entity is a persistence technology type"));

    private static final Set<String> PERSISTENCE_IMPORTS = Set.of(
            "org.apache.ibatis", "org.mybatis", "jakarta.persistence", "org.springframework.data.jpa");

    @Test
    void infrastructureRetainsOnlyConfigurationDatasourceTransactionAndMigration() throws IOException {
        Map<String, List<String>> unexpectedLeaves = new LinkedHashMap<>();

        for (String context : BOUNDED_CONTEXTS) {
            Path infrastructure = SERVER_ROOT.resolve(context).resolve("infrastructure");
            if (!Files.isDirectory(infrastructure)) {
                continue;
            }
            List<String> offending = new ArrayList<>();
            try (Stream<Path> children = Files.list(infrastructure)) {
                for (Path child : children.sorted().toList()) {
                    if (!Files.isDirectory(child)) {
                        offending.add(child.toString());
                        continue;
                    }
                    String leaf = child.getFileName().toString();
                    if (ALLOWED_LEAVES.containsKey(leaf)) {
                        continue;
                    }
                    // An empty directory is residue, not ownership. It cannot hold a violation and git
                    // does not track it, so failing on one would make this gate depend on a working
                    // copy's history rather than on the code.
                    if (isEmptyOfJava(child)) {
                        continue;
                    }
                    offending.add(child.toString());
                }
            }
            if (!offending.isEmpty()) {
                unexpectedLeaves.put(context, offending);
            }
        }

        assertThat(unexpectedLeaves)
                .as("these leaves are not on the infrastructure allowlist %s. A directory with no rule "
                        + "accumulates whatever does not obviously belong elsewhere, which is how the "
                        + "persistence models ended up under infrastructure in the first place. Widen "
                        + "the allowlist with a reason, or move the code.", ALLOWED_LEAVES.keySet())
                .isEmpty();
    }

    @Test
    void noPersistenceTechnologyTypeLivesUnderInfrastructureAgain() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String context : BOUNDED_CONTEXTS) {
            Path infrastructure = SERVER_ROOT.resolve(context).resolve("infrastructure");
            if (!Files.isDirectory(infrastructure)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(infrastructure)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .toList()) {
                    String name = file.getFileName().toString();
                    FORBIDDEN_SUFFIXES.forEach((suffix, reason) -> {
                        if (name.endsWith(suffix)) {
                            violations.add(file + " -- " + reason);
                        }
                    });

                    String source = Files.readString(file);
                    for (String forbidden : PERSISTENCE_IMPORTS) {
                        // A selector configuration legitimately names both providers' types in order to
                        // construct them; that is the composition root doing its job. The test targets
                        // classes that *use* persistence APIs, which is what an import of the API itself
                        // indicates.
                        if (source.contains("import " + forbidden)
                                && !name.endsWith("SelectorConfiguration.java")
                                && !name.equals("JpaPersistenceConfiguration.java")) {
                            violations.add(file + " imports " + forbidden
                                    + " -- persistence APIs belong to the outbound adapter, or to a "
                                    + "configuration class that wires them");
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .as("these files put persistence technology back under infrastructure. That is the exact "
                        + "state task 2.67 moved 53 files to end, and it matters because outside the "
                        + "adapter an application or domain class can import one of these without "
                        + "crossing any package a guard watches.")
                .isEmpty();
    }

    private static boolean isEmptyOfJava(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .noneMatch(p -> p.toString().endsWith(".java"));
        }
    }
}
