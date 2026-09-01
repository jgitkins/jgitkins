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
 * <p>{@link CrossContextAclContractArchitectureTest} forbids holding a foreign <em>aggregate</em>. That
 * left a hole one package wide: an MBG mapper and a JPA entity are not aggregates, so importing them
 * never tripped it, while the coupling is to the other context's table shape.
 *
 * <p>The guard that should have caught it had stopped working.
 * {@code ArchitecturePackageConventionTest} scanned {@code repository/infrastructure} for imports of
 * {@code io.jgitkins.server.infrastructure.persistence.*}, a package that no longer exists -- so it
 * reported clean while the file under its own root took on six foreign persistence imports. An import
 * scan whose subject is renamed away cannot tell a clean tree from an empty one.
 *
 * <p>Landed as a ratchet at 19, not a zero: a guard that arrives after the work it guards protected
 * nobody. The refactor took it to zero and the ceiling came with it. The mechanism stays for the next
 * boundary that has to be crossed on the way somewhere -- raise it, name the files, lower it again.
 * {@code PUBLIC_CEILING} in {@code RouteAuthenticationContractTest} works the same way.
 */
class CrossContextPersistenceCouplingArchitectureTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "repository", "execution", "identity/access", "change/review");

    /** Empty. Kept rather than deleted: an empty allowlist plus a zero ceiling is still the rule. */
    private static final List<String> ALLOWED = List.of();

    /** Zero, and it may not rise. It was 19 for the length of one commit. */
    private static final int COUPLING_CEILING = 0;

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
                .as("no context may read another context's tables through that context's mappers. The "
                        + "question belongs on a port owned by the context that owns the table -- see "
                        + "OrganizeMembershipQueryPort#findOrganizeIdsByUserId for the shape.")
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
        // Vacuous while ALLOWED is empty, and kept for the day it is not: an allowlist entry for a
        // deleted file is a permanent exemption that the next file at that path inherits.
        for (String allowed : ALLOWED) {
            assertThat(ArchitectureScanner.mainRoot().resolve(allowed))
                    .as("a stale allowlist entry is a permanent exemption for a file that is gone, and "
                            + "the next file to take its path inherits it")
                    .exists();
        }
    }

    @Test
    void theThreeFilesThatCarriedTheCouplingNowUseThePorts() throws IOException {
        // The count reaching zero does not prove the reads survived -- deleting them would score the
        // same. These three files must still answer the same three questions, through the ports.
        //
        // Matched on comment-stripped source, and on an import plus a use. A raw substring check passed
        // on the javadoc that explains the ports, so a field deletion leaving the prose behind stayed
        // green -- the pitfall ArchitectureScanner#stripComments exists for.
        for (String file : List.of(
                "repository/adapter/out/persistence/RepositoryPersistenceAdapter.java",
                "repository/adapter/out/persistence/jpa/RepositoryJpaPersistenceAdapter.java",
                "repository/infrastructure/config/RepositoryPersistenceSelectorConfiguration.java")) {
            String source = ArchitectureScanner.withoutComments(
                    ArchitectureScanner.mainRoot().resolve(file));
            for (String port : List.of(
                    "UserNamespacePort", "OrganizationNamespacePort", "OrganizationMembershipPort")) {
                assertThat(source)
                        .as("%s dropped its foreign persistence imports; it must have gained %s in code, "
                                + "not only in prose", file, port)
                        .contains("import io.jgitkins.server.repository.application.port.out." + port + ";")
                        .contains(Character.toLowerCase(port.charAt(0)) + port.substring(1));
            }
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
