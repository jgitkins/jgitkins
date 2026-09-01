package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.architecture.ArchitectureScanner.Violation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No context may read another context's tables through that context's own mappers.
 *
 * <p>{@link CrossContextAclContractArchitectureTest} forbids holding a foreign <em>aggregate</em>, and
 * its reasoning is that owning another context's invariants and lifecycle makes the two inseparable.
 * That rule left a hole exactly one package wide. A generated MBG mapper and a JPA entity are not
 * aggregates -- they have no invariants at all -- so importing them never tripped the guard, while the
 * coupling they create is worse: it is to the other context's <em>table shape</em>, and the other
 * context can no longer change its own storage without breaking this one.
 *
 * <p>The hole was not theoretical.
 * {@code ArchitecturePackageConventionTest#repositoryInfrastructureSources_doNotImportLegacyOrganizeInfrastructurePackages}
 * was written to watch for precisely this, against {@code io.jgitkins.server.infrastructure.persistence.*}.
 * That package no longer exists -- collaboration's persistence moved to
 * {@code collaboration.adapter.out.persistence.*} -- so the assertion has been passing over a
 * directory that cannot contain what it looks for, while the file it guards accumulated six foreign
 * persistence imports. A guard whose subject was renamed out from under it reports a clean tree and an
 * empty tree identically.
 *
 * <h2>Why a ceiling and not zero</h2>
 *
 * <p>Nineteen imports across three files exist today, all of them in {@code repository}. Asserting
 * zero would mean this test cannot land until the refactor does, and a guard that arrives after the
 * work it guards is a guard nobody was protected by. The ceiling makes the number visible and
 * one-directional: it may shrink, and lowering it is part of removing the coupling. It may not grow.
 *
 * <p>{@code PUBLIC_CEILING} in {@code RouteAuthenticationContractTest} works the same way and for the
 * same reason.
 */
class CrossContextPersistenceCouplingArchitectureTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "repository", "execution", "identity/access", "change/review");

    /**
     * Every file that currently reaches into another context's persistence, and how many times.
     *
     * <p>Listed by name rather than counted in aggregate so that moving the coupling from one file to
     * another is a failure rather than a wash. All three are the same refactor: {@code repository}
     * resolving a username, an organization name, and a user's organization ids by querying
     * {@code identity}'s and {@code collaboration}'s tables directly, when it already declares
     * {@code UserNamespacePort}, {@code OrganizationNamespacePort} and {@code OrganizationMembershipPort}
     * for two of those three questions and already has ACL adapters implementing them.
     */
    private static final List<String> ALLOWED = List.of(
            "repository/adapter/out/persistence/RepositoryPersistenceAdapter.java",
            "repository/adapter/out/persistence/jpa/RepositoryJpaPersistenceAdapter.java",
            "repository/infrastructure/config/RepositoryPersistenceSelectorConfiguration.java");

    /** May shrink, never grow. 19 when this test was written. */
    private static final int COUPLING_CEILING = 19;

    @Test
    void noContextReadsAnotherContextsTablesThroughItsMappers() throws IOException {
        List<Violation> violations = foreignPersistenceImports();

        Set<String> offendingFiles = new TreeSet<>();
        for (Violation violation : violations) {
            offendingFiles.add(relative(violation.file()));
        }

        assertThat(offendingFiles)
                .as("a foreign persistence import couples this context to another's table shape, and no "
                        + "layer is the right place for it -- an ACL translates through the other "
                        + "context's application port instead. New entries belong in the refactor, not "
                        + "in this list.")
                .isSubsetOf(ALLOWED);
    }

    @Test
    void theCouplingOnlyShrinks() throws IOException {
        List<Violation> violations = foreignPersistenceImports();

        assertThat(violations)
                .as("foreign persistence imports may only decrease. If this fails low, lower "
                        + "COUPLING_CEILING in the same commit -- that is how the ratchet tightens. If it "
                        + "fails high, the import that was just added belongs behind a port.")
                .hasSizeLessThanOrEqualTo(COUPLING_CEILING);
    }

    @Test
    void theCeilingIsNotSlackerThanTheTree() throws IOException {
        assertThat(foreignPersistenceImports())
                .as("a ceiling well above the real count stops being a ratchet and becomes a comment. "
                        + "Lower COUPLING_CEILING to the actual number.")
                .hasSize(COUPLING_CEILING);
    }

    @Test
    void everyAllowlistedFileStillExists() {
        for (String allowed : ALLOWED) {
            assertThat(ArchitectureScanner.mainRoot().resolve(allowed))
                    .as("a stale allowlist entry is a permanent exemption for a file that is gone, and "
                            + "the next file to take its path inherits it")
                    .exists();
        }
    }

    @Test
    void theForeignPersistenceCategoryActuallyFires() throws IOException {
        List<Violation> found = ArchitectureScanner.scan(
                ArchitectureScanner.negativeFixtures().resolve("foreign-persistence-import.java"),
                List.of(ArchitectureScanner.FORBIDDEN_FOREIGN_PERSISTENCE));

        assertThat(found)
                .as("the fixture injects collaboration's MBG mapper into a repository adapter; if the "
                        + "category does not match it, every assertion above is vacuous")
                .hasSize(1);
    }

    @Test
    void aContextsOwnPersistenceIsNotForeign() throws IOException {
        // The category matches any context's persistence package, so the own-context filter below is
        // load-bearing. Without it every persistence adapter in the tree would be a violation and the
        // ceiling would be meaningless.
        Path ownAdapter = ArchitectureScanner.mainRoot()
                .resolve("collaboration/adapter/out/persistence/OrganizePersistenceAdapter.java");
        assertThat(ownAdapter).exists();

        assertThat(foreignPersistenceImports().stream().map(v -> relative(v.file())).toList())
                .as("collaboration's own persistence adapter imports collaboration persistence types, "
                        + "and that is not a boundary crossing")
                .doesNotContain("collaboration/adapter/out/persistence/OrganizePersistenceAdapter.java");
    }

    /** Every foreign persistence import in main, own-context imports filtered out. */
    private static List<Violation> foreignPersistenceImports() throws IOException {
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
                        .sorted()
                        .toList()) {
                    for (Violation violation : ArchitectureScanner.scan(
                            file, List.of(ArchitectureScanner.FORBIDDEN_FOREIGN_PERSISTENCE))) {
                        if (!violation.line().contains(ownPackage)) {
                            violations.add(violation);
                        }
                    }
                }
            }
        }
        return violations;
    }

    private static String relative(Path file) {
        return ArchitectureScanner.mainRoot().relativize(file).toString();
    }
}
