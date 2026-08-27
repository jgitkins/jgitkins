package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.architecture.ArchitectureScanner.Violation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A context may talk to another context's ports. It may not hold another context's aggregates.
 *
 * <p>The distinction is the whole of the ACL rule. Calling {@code OrganizeQueryPort} asks a question and
 * gets an answer. Holding an {@code Organize} means this context now has an object whose invariants
 * another context enforces, whose lifecycle it does not control, and whose changes will silently
 * propagate — so the two contexts stop being separable and one of them starts owning the other's rules.
 *
 * <p>Cross-context access is confined to {@code adapter/out/acl}, where it is a translation with a name.
 * Anywhere else, a foreign aggregate import is the boundary quietly dissolving.
 *
 * <p>This test deliberately does not restate
 * {@code ArchitecturePackageConventionTest#identityCollaborationAclBoundary_hasExactProductionAndFixtureAllowlist},
 * which pins the identity-to-collaboration import count at exactly two. Duplicating that number here
 * would give it two homes that could disagree; this test asserts the kind of import, that one asserts
 * the count.
 */
class CrossContextAclContractArchitectureTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "repository", "execution", "identity/access", "change/review");

    @Test
    void forbidsForeignAggregateLifecycleImports() throws IOException {
        List<Violation> violations = new ArrayList<>();

        for (String context : CONTEXTS) {
            Path root = ArchitectureScanner.mainRoot().resolve(context);
            if (!Files.isDirectory(root)) {
                continue;
            }
            String ownPackage = "io.jgitkins.server." + context.replace('/', '.') + ".";

            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.toString().contains("/adapter/out/acl/"))
                        .sorted()
                        .toList()) {
                    for (Violation violation : ArchitectureScanner.scan(
                            file, List.of(ArchitectureScanner.FORBIDDEN_FOREIGN_AGGREGATE))) {
                        // Its own aggregates are not foreign. The category matches any context's
                        // aggregate package, so ownership has to be decided here, where the owner is known.
                        if (!violation.line().contains(ownPackage)) {
                            violations.add(violation);
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .as("cross-context access belongs in adapter/out/acl, where it is a translation with a "
                        + "name. Holding another context's aggregate means owning invariants you do not "
                        + "enforce and a lifecycle you do not control.")
                .isEmpty();
    }

    @Test
    void theForeignAggregateCategoryActuallyFires() throws IOException {
        List<Violation> found = ArchitectureScanner.scan(
                ArchitectureScanner.negativeFixtures().resolve("foreign-aggregate-import.java"),
                List.of(ArchitectureScanner.FORBIDDEN_FOREIGN_AGGREGATE));

        assertThat(found)
                .as("the fixture injects a collaboration aggregate into a repository service; if the "
                        + "category does not match it, the guard above proves nothing")
                .hasSize(1);
    }

    @Test
    void aclAdaptersAreWhereCrossContextAccessIsAllowedToLive() throws IOException {
        // The rule above is an exclusion, and an exclusion over an empty set is vacuous. If no ACL
        // adapter existed, the test would pass by having nothing to exclude.
        List<Path> aclAdapters = new ArrayList<>();
        for (String context : CONTEXTS) {
            Path acl = ArchitectureScanner.mainRoot().resolve(context).resolve("adapter/out/acl");
            if (Files.isDirectory(acl)) {
                try (Stream<Path> files = Files.walk(acl)) {
                    aclAdapters.addAll(files.filter(p -> p.toString().endsWith(".java")).toList());
                }
            }
        }
        assertThat(aclAdapters)
                .as("no ACL adapter exists, so the exclusion above excludes nothing and the test is "
                        + "vacuous rather than passing")
                .isNotEmpty();
    }
}
