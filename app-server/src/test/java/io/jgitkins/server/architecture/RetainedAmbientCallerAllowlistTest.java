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
 * The ambient actor ports still exist, and exactly three production classes may use them.
 *
 * <p>Tasks 2.63 through 2.65 removed ambient actor access from every business decision. They did not
 * delete the ports: something has to read the security context, and that something is an outbound
 * adapter whose entire job is to answer "who is the caller". Those three are named here.
 *
 * <p>An allowlist rather than a ban, because a ban would be false — the ports are load-bearing — and a
 * ban that had to be worked around would be removed. An allowlist makes the fourth use a visible
 * decision: adding a name here is a line in a diff that says what it is.
 *
 * <p>The list is exact in both directions. An extra entry is a new ambient reader; a missing one means an
 * adapter stopped reading the context, which is worth noticing too, because something else must now be
 * supplying the actor and nobody wrote that down.
 */
class RetainedAmbientCallerAllowlistTest {

    /** The only production classes permitted to read an ambient actor, each with why. */
    private static final Set<String> ALLOWLISTED = new TreeSet<>(Set.of(
            // Reads SecurityContextHolder and parses the subject to a user id. This is the one adapter
            // whose purpose is exactly that, and every explicit requester in the system starts here.
            "CurrentUserSecurityAdapter.java",
            // Answers "is the current account active" for the policy port; the question is about the
            // ambient caller by definition.
            "ActiveAccountPolicyAdapter.java",
            // Collaboration's implementation of the same current-user port, for its own context.
            "InProcessUserIdentityAdapter.java",
            // The repository context's outbound implementation of RepositoryActorPort. It *is* the port,
            // so it necessarily names it.
            "RepositoryActorAclAdapter.java",
            // Declares the port. A port naming its own type is not a use of an ambient actor.
            "CurrentUserPort.java",
            "RepositoryActorPort.java"));

    @Test
    void matchesExactRetainedCallers() throws IOException {
        Set<String> actual = new TreeSet<>();
        List<Violation> violations = ArchitectureScanner.scanTree(
                List.of(ArchitectureScanner.mainRoot()),
                List.of(ArchitectureScanner.FORBIDDEN_CURRENT_USER,
                        ArchitectureScanner.FORBIDDEN_REPOSITORY_ACTOR));

        for (Violation violation : violations) {
            actual.add(violation.file().getFileName().toString());
        }

        assertThat(actual)
                .as("exactly these production classes may read an ambient actor. An extra entry is a new "
                        + "ambient reader and undoes a piece of tasks 2.63-2.65; a missing one means an "
                        + "adapter stopped reading the context and something undocumented now supplies "
                        + "the actor.")
                .isEqualTo(ALLOWLISTED);
    }

    @Test
    void everyAllowlistedFileStillExists() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String fileName : ALLOWLISTED) {
            try (Stream<Path> files = Files.walk(ArchitectureScanner.mainRoot())) {
                if (files.noneMatch(p -> p.getFileName().toString().equals(fileName))) {
                    missing.add(fileName);
                }
            }
        }
        assertThat(missing)
                .as("an allowlist entry for a file that no longer exists reads as a live exception and is "
                        + "not one; it also silently applies to whatever takes that name next")
                .isEmpty();
    }

    @Test
    void bothAmbientCategoriesActuallyFire() throws IOException {
        Path fixtures = ArchitectureScanner.negativeFixtures();

        assertThat(ArchitectureScanner.scan(fixtures.resolve("application-current-user-import.java"),
                List.of(ArchitectureScanner.FORBIDDEN_CURRENT_USER)))
                .as("the CurrentUserPort category must match its fixture, or the allowlist above is "
                        + "comparing an empty set to an empty set")
                .hasSize(1);
        assertThat(ArchitectureScanner.scan(fixtures.resolve("application-actor-import.java"),
                List.of(ArchitectureScanner.FORBIDDEN_REPOSITORY_ACTOR)))
                .hasSize(1);
    }
}
