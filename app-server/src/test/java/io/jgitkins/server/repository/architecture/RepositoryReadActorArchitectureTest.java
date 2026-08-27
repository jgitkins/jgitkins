package io.jgitkins.server.repository.architecture;

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
 * The repository read boundary must not reach for an ambient actor.
 *
 * <p>Tasks 2.63 through 2.65 moved actor derivation to the inbound adapters one route at a time. Each
 * step removed a specific call; nothing stopped the next author from adding one back, and the symptom
 * would not be a failing test — it would be a read authorized against whoever the security context
 * happened to hold, which is correct in every single-user test and wrong under concurrency.
 *
 * <p>The rule is scoped to {@code application/service}, {@code application/validate} and
 * {@code application/policy}: business decisions consume an explicit requester. Adapters are excluded
 * because deriving the actor is exactly their job, and {@code RepositoryActorAclAdapter} is the outbound
 * implementation of the port itself.
 */
class RepositoryReadActorArchitectureTest {

    private static final Path REPOSITORY_ROOT = resolveRoot();

    private static final List<String> GUARDED_ROOTS = List.of(
            "application/service", "application/validate", "application/policy", "application/support");

    /** Each forbidden token, with what its presence would mean. */
    private static final Map<String, String> FORBIDDEN = Map.of(
            "RepositoryActorPort", "the ambient actor port; the requester is an argument now",
            "CurrentUserPort", "identity's ambient actor port, reachable from this context too",
            "SecurityContextHolder", "reading the security context inside a business decision",
            "RequestContextHolder", "reading servlet state inside a business decision",
            "jakarta.servlet", "servlet types have no business in the application layer",
            "io.jsonwebtoken", "JWT types have no business in the application layer");

    @Test
    void forbidsAmbientActorInReadBoundary() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String guarded : GUARDED_ROOTS) {
            Path root = REPOSITORY_ROOT.resolve(guarded);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .toList()) {
                    String source = stripComments(Files.readString(file));
                    FORBIDDEN.forEach((token, why) -> {
                        if (source.contains(token)) {
                            violations.add(file.getFileName() + " -> " + token + " (" + why + ")");
                        }
                    });
                }
            }
        }

        assertThat(violations)
                .as("the repository read and decision boundary must take its actor as an argument. Every "
                        + "entry here is a business decision that would authorize against whoever the "
                        + "security context happened to hold -- correct in a single-user test, wrong "
                        + "under concurrency, and invisible either way.")
                .isEmpty();
    }

    @Test
    void theActorPortStillExistsForTheAdaptersThatLegitimatelyUseIt() {
        // The rule above is about where the port may be *consumed*, not about deleting it. Task 2.65 did
        // not retire RepositoryActorPort; asserting its absence would overstate what was done and would
        // fail the moment someone reads the plan and looks for it.
        Path port = REPOSITORY_ROOT.resolve("application/port/out/RepositoryActorPort.java");
        assertThat(Files.exists(port))
                .as("RepositoryActorPort is retained; %s", port)
                .isTrue();
    }

    /**
     * Removes block and line comments before scanning.
     *
     * <p>Several of these classes carry a javadoc line saying which port they no longer use. That is
     * documentation of the fix, not a use of the port, and a scanner that failed on it would make
     * deleting the explanation the obvious way to go green -- exactly the wrong incentive.
     */
    private static String stripComments(String source) {
        String withoutBlocks = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlocks.replaceAll("(?m)//.*$", "");
    }

    private static Path resolveRoot() {
        Path local = Path.of("src/main/java/io/jgitkins/server/repository");
        return Files.isDirectory(local)
                ? local
                : Path.of("app-server/src/main/java/io/jgitkins/server/repository");
    }
}
