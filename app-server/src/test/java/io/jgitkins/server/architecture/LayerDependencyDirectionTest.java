package io.jgitkins.server.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Which direction a dependency is allowed to point. Nothing here asserts where a file lives.
 *
 * <p>These eight rules were extracted from {@code ArchitecturePackageConventionTest} before that
 * file was deleted. It held two different kinds of assertion under one name: about eleven that
 * spelled out which package each class belongs to, and these, which name a source root and an
 * import prefix and say the second must not appear under the first. The first kind is a second copy
 * of the package tree -- move a class and the test has to move with it. Measured over the thirty
 * three commits that touched the architecture tests, twenty three were refactors editing a test to
 * permit a move rather than catching one, and today's package rename had to edit four test files.
 * These eight needed no edit, because an import prefix does not care where the importer sits.
 *
 * <p>So the split is deliberate: placement went away with the file, direction moved here.
 *
 * <pre>
 *   core-*        --X-->  io.jgitkins.server / .web / .runner     (coreModules_...)
 *   app-web       --X-->  core.web.api.response.ApiResponse       (webMvcControllers_...)
 *   application   --X-->  infrastructure                          (repositoryApplication..., identityAccess...)
 *   adapter/in    --X-->  collaboration.infrastructure            (collaborationInboundAdapters_...)
 *   adapter/out/git --X-->  application.exception                 (repositoryGitAdapters_...)
 *   core-persistence  must own no model/entity/adapter package    (corePersistence_...)
 * </pre>
 *
 * <p>Two of these forbid {@code io.jgitkins.server.infrastructure.}, a top-level package that task
 * 2.67 emptied and that no longer exists. They are ratchets rather than live guards: cheap to keep,
 * and they fail loudly if the package is recreated. {@code resolveExistingPath} throws when none of
 * its candidates exist, so a rule whose scan root is renamed away fails instead of silently passing
 * over nothing -- the failure mode that let a guard in this repo scan a renamed-away package and
 * stay green.
 */
class LayerDependencyDirectionTest {

    @Test
    void collaborationInboundAdapters_doNotImportInfrastructure() throws IOException {
        Path inboundRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/collaboration/adapter/in",
                "app-server/src/main/java/io/jgitkins/server/collaboration/adapter/in");
        assertNoImports(inboundRoot, "import io.jgitkins.server.collaboration.infrastructure.");
    }

    @Test
    void repositoryApplicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path repositoryApplicationRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/repository/application",
                "app-server/src/main/java/io/jgitkins/server/repository/application");
        assertNoInfrastructureImports(repositoryApplicationRoot);
    }

    @Test
    void collaborationInfrastructureSources_doNotImportTopLevelInfrastructurePersistencePackages() throws IOException {
        Path collaborationInfrastructureRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/collaboration/infrastructure",
                "app-server/src/main/java/io/jgitkins/server/collaboration/infrastructure");
        assertNoImports(collaborationInfrastructureRoot, "import io.jgitkins.server.infrastructure.persistence.");
    }

    @Test
    void identityAccessApplicationSources_doNotImportInfrastructurePackages() throws IOException {
        Path identityAccessApplicationRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/identity/access/application",
                "app-server/src/main/java/io/jgitkins/server/identity/access/application");
        assertNoInfrastructureImports(identityAccessApplicationRoot);
        assertNoImports(identityAccessApplicationRoot, "import io.jgitkins.server.collaboration.");
    }

    @Test
    void repositoryGitAdapters_doNotImportRepositoryApplicationExceptions() throws IOException {
        Path repositoryGitAdapterRoot = resolveExistingPath(
                "src/main/java/io/jgitkins/server/repository/adapter/out/git",
                "app-server/src/main/java/io/jgitkins/server/repository/adapter/out/git");
        assertNoImports(repositoryGitAdapterRoot, "import io.jgitkins.server.repository.application.exception.");
    }

    @Test
    void coreModules_doNotImportApplicationModules() throws IOException {
        List<Path> coreRoots = Stream.of(
                        "core-common/src/main/java",
                        "core-web/src/main/java",
                        "core-security/src/main/java",
                        "core-persistence/src/main/java",
                        "core-grpc/src/main/java")
                .flatMap(candidate -> Stream.of(Path.of("../" + candidate), Path.of(candidate)))
                .filter(Files::exists)
                .toList();

        // A typo in every candidate path would leave this list empty and the loop would assert
        // nothing, which is the shape of a guard that passes because it looked at no files.
        assertFalse(coreRoots.isEmpty(), () -> "No core module source root resolved; the rule examined nothing");

        for (Path coreRoot : coreRoots) {
            assertNoImports(coreRoot, "import io.jgitkins.server.");
            assertNoImports(coreRoot, "import io.jgitkins.web.");
            assertNoImports(coreRoot, "import io.jgitkins.runner.");
        }
    }

    @Test
    void webMvcControllers_doNotUseCoreApiResponseAsViewModel() throws IOException {
        Path webPresentationRoot = resolveExistingPath(
                "../app-web/src/main/java/io/jgitkins/web/presentation",
                "app-web/src/main/java/io/jgitkins/web/presentation");
        assertNoImports(webPresentationRoot, "import io.jgitkins.core.web.api.response.ApiResponse;");
    }

    @Test
    void corePersistence_doesNotOwnBusinessPersistenceModels() throws IOException {
        Path corePersistenceRoot = resolveExistingPath(
                "../core-persistence/src/main/java",
                "core-persistence/src/main/java");

        assertNoPath(corePersistenceRoot, "model");
        assertNoPath(corePersistenceRoot, "entity");
        assertNoPath(corePersistenceRoot, "adapter");
    }

    /**
     * Throws rather than returning empty when nothing resolves. A rule that cannot find its own
     * scan root must fail, not pass over zero files.
     */
    private Path resolveExistingPath(String... candidates) {
        return Arrays.stream(candidates)
                .map(Path::of)
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to resolve existing path from candidates: "
                        + String.join(", ", candidates)));
    }

    private void assertNoInfrastructureImports(Path root) throws IOException {
        assertNoImports(root, "import io.jgitkins.server.infrastructure.");
    }

    private void assertNoImports(Path root, String disallowedImportPrefix) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> javaFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            // Walking a directory that exists but holds no java sources would assert nothing.
            assertFalse(javaFiles.isEmpty(),
                    () -> "No java sources under " + root + "; the rule examined nothing");

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile);
                assertFalse(source.lines().anyMatch(line -> line.startsWith(disallowedImportPrefix)),
                        () -> "Source must not import " + disallowedImportPrefix + ": " + javaFile);
            }
        }
    }

    private void assertNoPath(Path root, String disallowedPathSegment) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            boolean hasDisallowedPath = files
                    .anyMatch(path -> path.getNameCount() > 0
                            && path.toString().contains("/" + disallowedPathSegment + "/"));

            assertFalse(hasDisallowedPath,
                    () -> "Path must not contain segment " + disallowedPathSegment + ": " + root);
        }
    }
}
