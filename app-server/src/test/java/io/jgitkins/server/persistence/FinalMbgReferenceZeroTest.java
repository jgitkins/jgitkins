package io.jgitkins.server.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Task 2.77 removal gate: no MyBatis or MBG asset may be deleted while anything still references it.
 *
 * <p>Task 2.77 deletes nothing. Its manifest disposition for every one of the 25 frozen assets is
 * {@code RETAINED_BASELINE} or {@code DEFERRED_UNTIL_REFERENCE_ZERO}, and no {@code REMOVED} disposition
 * is authorised. This test is the gate that keeps it that way, because the pressure to "finish the
 * migration" by deleting the old mappers is exactly what a retirement task attracts.
 *
 * <p>Reference count is not zero today and is not supposed to be. Every capability selector still
 * constructs its MyBatis adapter — that construction is the rollback. Deleting a mapper whose adapter is
 * still reachable would not fail the build; it would fail at runtime, the first time an operator rolled
 * a capability back, which is the worst possible moment to discover it.
 *
 * <p>So the assertion is conditional, not absolute: an asset with references must still exist. That
 * makes the test fail on a premature deletion and pass on a legitimate one, rather than freezing the
 * tree forever.
 */
class FinalMbgReferenceZeroTest {

    private static final Path REPOSITORY_ROOT = Paths.get("..").toAbsolutePath().normalize();

    /** The XML resources and shared configuration this task freezes, relative to the repository root. */
    private static final List<String> FROZEN_ASSETS = List.of(
            "app-runner/src/main/resources/mapper/mbg/JobHistoryEntityMbgMapper.xml",
            "app-runner/src/main/resources/mapper/mbg/RunnerConfigEntityMbgMapper.xml",
            "app-runner/src/main/resources/mapper/mbg/RunnerConfigFileEntityMbgMapper.xml",
            "app-runner/src/main/resources/mapper/mbg/RunnerEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/custom/JobDispatchQueryMapper.xml",
            "app-server/src/main/resources/mapper/mbg/BranchEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/JobEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/JobHistoryEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/OrganizeEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/OrganizeMemberEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/PullRequestEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/RepositoryEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/RepositoryMemberEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/RunnerAssignmentEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/RunnerEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/UserCredentialsEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/UserEntityMbgMapper.xml",
            "app-server/src/main/resources/mapper/mbg/UserIdentitiesEntityMbgMapper.xml",
            "core-persistence/src/main/java/io/jgitkins/core/persistence/DataSourceConfig.java",
            "core-persistence/src/main/java/io/jgitkins/core/persistence/MybatisConfig.java");

    private static final List<String> SEARCH_ROOTS = List.of("app-server", "app-runner", "core-persistence");

    @Test
    void allMbgAssetsHaveZeroReferencesBeforeRemoval() throws IOException {
        Map<String, Long> referencesByMissingAsset = new LinkedHashMap<>();
        int present = 0;

        for (String asset : FROZEN_ASSETS) {
            Path path = REPOSITORY_ROOT.resolve(asset);
            String symbol = stem(asset);
            long references = countReferences(symbol, asset);

            if (Files.exists(path)) {
                present++;
                continue;
            }
            if (references > 0) {
                referencesByMissingAsset.put(asset, references);
            }
        }

        assertThat(referencesByMissingAsset)
                .as("these assets were deleted while still referenced. The deletion does not break the "
                        + "build — the references are Spring/MyBatis wiring resolved at runtime — so it "
                        + "would first surface when an operator rolls a capability back to MyBatis, which "
                        + "is the worst possible moment. Restore them, or remove the references first.")
                .isEmpty();

        assertThat(present)
                .as("the frozen inventory is the rollback target for every capability migrated in tasks "
                        + "2.70-2.76; if none of it is present the audit is measuring nothing")
                .isGreaterThan(0);
    }

    @Test
    void theMybatisAdaptersEachCapabilityRollsBackToAreStillReachable() throws IOException {
        // Named individually rather than discovered: these are the exact rollback targets recorded in the
        // 2.70-2.76 plans. If a future task removes one, this list is where the contradiction shows up.
        List<String> rollbackAdapters = List.of(
                "app-server/src/main/java/io/jgitkins/server/collaboration/adapter/out/persistence/OrganizePersistenceAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/identity/access/adapter/out/persistence/UserPersistenceAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/repository/adapter/out/persistence/RepositoryPersistenceAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/execution/adapter/out/persistence/JobRepositoryAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/execution/adapter/out/persistence/RunnerPersistenceAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/execution/adapter/out/persistence/JobDispatchQueryAdapter.java",
                "app-server/src/main/java/io/jgitkins/server/change/review/adapter/out/persistence/PullRequestPersistenceAdapter.java");

        for (String adapter : rollbackAdapters) {
            Path path = REPOSITORY_ROOT.resolve(adapter);
            assertThat(Files.exists(path))
                    .as("%s is the MyBatis side of a selector. Removing it makes that capability's "
                            + "documented rollback impossible, and the selector's mybatis branch would "
                            + "not compile.", adapter)
                    .isTrue();

            String source = Files.readString(path);
            // Two checks rather than a substring search for "@Component". Several of these adapters carry
            // a javadoc line explaining that they are deliberately *not* components, and a naive
            // contains() matched that prose — a test that fails on its own documentation is worse than no
            // test, because the obvious fix is to delete the explanation.
            assertThat(source)
                    .as("%s must not import the stereotype: the annotation cannot be applied without it, "
                            + "so its absence is the unambiguous signal", adapter)
                    .doesNotContain("import org.springframework.stereotype.Component;");
            assertThat(annotationLines(source))
                    .as("%s must not carry @Component as an annotation: with a JPA implementation of the "
                            + "same port on the classpath, scanning would register two candidates and the "
                            + "injection point would be ambiguous. The composition root names exactly one.",
                            adapter)
                    .noneMatch(line -> line.equals("@Component") || line.startsWith("@Component("));
        }
    }

    /** Annotation-position lines only: leading-asterisk javadoc and {@code //} comments are dropped. */
    private static List<String> annotationLines(String source) {
        return source.lines()
                .map(String::strip)
                .filter(line -> line.startsWith("@"))
                .toList();
    }

    private static long countReferences(String symbol, String selfPath) throws IOException {
        long total = 0;
        for (String root : SEARCH_ROOTS) {
            Path rootPath = REPOSITORY_ROOT.resolve(root);
            if (!Files.isDirectory(rootPath)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(rootPath)) {
                total += files
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".yml");
                        })
                        .filter(path -> !REPOSITORY_ROOT.relativize(path).toString().equals(selfPath))
                        .filter(path -> !path.toString().contains("/build/"))
                        .filter(path -> contains(path, symbol))
                        .count();
            }
        }
        return total;
    }

    private static boolean contains(Path path, String symbol) {
        try {
            return Files.readString(path).contains(symbol);
        } catch (IOException e) {
            // An unreadable file is not evidence of absence, so it must not be counted as a zero.
            throw new IllegalStateException("could not read " + path + " while counting references", e);
        }
    }

    private static String stem(String asset) {
        String name = Paths.get(asset).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
