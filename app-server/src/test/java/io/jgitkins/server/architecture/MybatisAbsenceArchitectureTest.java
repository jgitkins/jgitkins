package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * MyBatis is gone from every module's production code, and stays gone.
 *
 * <p>Replaces {@code FinalMbgReferenceZeroTest}, whose assertion was conditional in both directions:
 * a listed asset with references had to still exist, and the seven MyBatis adapters had to still be
 * reachable without {@code @Component}. Both halves protected a rollback, and the rollback was given
 * up. The remaining risk is the opposite one -- MyBatis returning by accident, through a copied
 * mapper or a dependency someone re-adds to make an example compile -- so this is absolute.
 *
 * <p>Lives beside the other architecture guards rather than under {@code persistence}, where the
 * 2.77 audit tests were, because that is where {@link ArchitectureScanner} is -- and its
 * comment-stripping is what keeps this from failing on prose about MyBatis.
 *
 * <p><strong>Scoped to {@code src/main} on purpose.</strong> Test sources name MyBatis legitimately:
 * {@code BoundedContextDomainPurityArchitectureTest} forbids {@code org.mybatis} imports under the
 * domain roots and keeps {@code architecture/negative/application-mybatis-import.java} as the
 * positive control that proves the category still fires. That rule outlives the provider and its
 * fixture has to contain the thing it forbids. A guard that cannot tell a forbidden import from a
 * guard against it would be answered by deleting the other guard.
 *
 * <p><strong>Two defects in the version this replaces are fixed here.</strong>
 *
 * <p>It counted references by filename stem across three modules at once. Deleting app-runner's four
 * mapper XMLs was therefore reported as four deletions-while-still-referenced, the references being
 * app-server files that mention its own identically named {@code JobHistoryEntityMbgMapper}. A
 * module-blind name match inside a guard about module contents. This one does not match on names --
 * it looks for the technology, which cannot be confused between modules.
 *
 * <p>And it walked the source tree, which Gradle cannot see as a task input. The app-runner commit
 * that deleted those XMLs reported BUILD SUCCESSFUL with {@code :app-server:test UP-TO-DATE} -- the
 * module holding the guard had no changed input, so the guard never ran against the tree it
 * inspects, and a red commit was recorded as green. Nothing inside a test can fix that. What it can
 * do is refuse to pass on an empty walk, so a mis-resolved root fails instead of scoring zero
 * violations: every assertion below is preceded by a count.
 */
class MybatisAbsenceArchitectureTest {

    private static final Path REPOSITORY_ROOT = Paths.get("..").toAbsolutePath().normalize();

    /** Every module. A list of the ones that used to have adapters would go stale on the next one. */
    private static final List<String> MODULES = List.of(
            "app-server", "app-runner", "app-web",
            "core-common", "core-grpc", "core-persistence", "core-security", "core-web");

    private static final List<String> FORBIDDEN_IN_SOURCE = List.of(
            "import org.apache.ibatis",
            "import org.mybatis",
            "SqlSessionFactory",
            "@MapperScan");

    @Test
    void noModulesProductionCodeUsesMybatis() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (Path file : mainSources()) {
            scanned++;
            // Comment-stripped through ArchitectureScanner, the same helper the other guards use.
            // JpaPersistenceConfiguration's javadoc explains that MybatisConfig used to build the
            // SqlSessionFactory by hand, which is why declaring the JPA beans by hand was consistent at
            // the time -- a guard that trips on the history of its own subject gets answered by
            // deleting the history. Reusing the helper rather than writing a second stripper is the
            // point of it being one method: the block-comment case is the one a quick version misses.
            String content = ArchitectureScanner.withoutComments(file);
            for (String forbidden : FORBIDDEN_IN_SOURCE) {
                if (content.contains(forbidden)) {
                    offenders.add(REPOSITORY_ROOT.relativize(file) + " -> " + forbidden);
                }
            }
        }

        assertFalse(scanned == 0, "no java sources found under any module's src/main; examined nothing");
        assertThat(offenders)
                .as("both providers on the classpath was a migration state with a selector behind it, "
                        + "and there is no selector to put a second one behind now: an unconditional "
                        + "@Mapper alongside a JPA implementation of the same port is an ambiguous "
                        + "injection point at startup, not a choice.")
                .isEmpty();
    }

    @Test
    void noModuleShipsGeneratedMappersOrMapperResources() throws IOException {
        List<String> found = new ArrayList<>();
        int scanned = 0;

        for (String module : MODULES) {
            Path main = REPOSITORY_ROOT.resolve(module).resolve("src/main");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(main)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    scanned++;
                    String relative = REPOSITORY_ROOT.relativize(file).toString();
                    if (file.getFileName().toString().contains("MbgMapper")
                            || relative.contains("/resources/mapper/")) {
                        found.add(relative);
                    }
                }
            }
        }

        assertFalse(scanned == 0, "no files found under any module's src/main; examined nothing");
        assertThat(found)
                .as("a file reappearing here means a mapper was copied back or MyBatis Generator was run "
                        + "again. The mapper-locations property is gone too, so the XML would be loaded "
                        + "by nothing -- it would sit in the jar looking authoritative and answering "
                        + "no query.")
                .isEmpty();
    }

    @Test
    void noModuleDeclaresAMybatisDependency() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (String module : MODULES) {
            Path build = REPOSITORY_ROOT.resolve(module).resolve("build.gradle");
            if (!Files.isRegularFile(build)) {
                continue;
            }
            scanned++;
            // Comment-stripped. core-persistence carries a comment naming the dependency it lost and
            // why, and a guard that fails on the explanation of its own subject gets fixed by deleting
            // the explanation.
            String declarations = Files.readString(build).lines()
                    .filter(line -> !line.strip().startsWith("//"))
                    .reduce("", (a, b) -> a + "\n" + b);
            if (declarations.contains("mybatis")) {
                offenders.add(module + "/build.gradle");
            }
        }

        assertFalse(scanned == 0, "no build files found; examined nothing");
        assertThat(offenders)
                .as("the dependency is what makes an accidental @Mapper compile. core-persistence "
                        + "declared it as `api`, so it reached app-server's compile classpath without "
                        + "app-server asking for it -- which is why every module is checked and not "
                        + "only the ones that had adapters.")
                .isEmpty();
    }

    private static List<Path> mainSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        for (String module : MODULES) {
            Path main = REPOSITORY_ROOT.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(main)) {
                sources.addAll(files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList());
            }
        }
        return sources;
    }
}
