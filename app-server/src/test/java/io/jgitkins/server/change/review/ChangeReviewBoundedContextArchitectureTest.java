package io.jgitkins.server.change.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ChangeReviewBoundedContextArchitectureTest {
    private static final Path PROJECT = Files.exists(Path.of("app-server")) ? Path.of(".") : Path.of("..");
    private static final Path MAIN = PROJECT.resolve("app-server/src/main/java");
    private static final Path TEST = PROJECT.resolve("app-server/src/test/java");
    private static final Path THIS = PROJECT.resolve("app-server/src/test/java/io/jgitkins/server/change/review/ChangeReviewBoundedContextArchitectureTest.java");

    @Test
    void sourceOwnershipAndForeignImportBoundariesAreExplicit() throws IOException {
        List<Path> production = javaFiles(MAIN);
        List<Path> tests = javaFiles(TEST);
        List<Path> all = Stream.concat(production.stream(), tests.stream()).toList();
        Map<Path, String> sources = all.stream().collect(Collectors.toMap(path -> path, this::read));

        Set<String> forbiddenDomainApplication = Set.of(
                "org.springframework", "org.mybatis", "org.eclipse.jgit", "jakarta.persistence",
                "io.grpc", "io.jgitkins.server.common.infrastructure", "io.jgitkins.server.repository.domain", "io.jgitkins.server.repository.application",
                "io.jgitkins.server.identity", "io.jgitkins.server.collaboration");
        sources.forEach((path, source) -> {
            String normalized = path.toString().replace('\\', '/');
            if (normalized.contains("/change/review/domain/")) {
                forbiddenDomainApplication.forEach(token -> assertThat(source).as(path.toString()).doesNotContain(token));
            }
            if (normalized.contains("/change/review/application/")) {
                Set<String> applicationForbidden = Set.of(
                        "org.mybatis", "org.eclipse.jgit", "jakarta.persistence", "io.grpc",
                        "io.jgitkins.server.common.infrastructure", "io.jgitkins.server.repository.domain", "io.jgitkins.server.repository.application",
                        "io.jgitkins.server.identity", "io.jgitkins.server.collaboration",
                        "RepositoryNamespaceResolver", "BranchGitPort", "MergeGitPort");
                applicationForbidden.forEach(token -> assertThat(source).as(path.toString()).doesNotContain(token));
            }
            if (normalized.contains("/change/review/domain/") || normalized.contains("/change/review/application/")
                    || normalized.contains("/change/review/infrastructure/") || normalized.contains("/change/review/presentation/")) {
                assertThat(source).as(path.toString()).doesNotContain("MergeGitPort");
            }
            if (normalized.contains("/change/review/")) {
                boolean foreignImport = source.lines().anyMatch(line -> line.startsWith("import io.jgitkins.server.repository.")
                        || line.startsWith("import io.jgitkins.server.identity.")
                        || line.startsWith("import io.jgitkins.server.collaboration."));
                boolean allowlisted = normalized.endsWith("/adapter/out/acl/RepositoryReferenceAclAdapter.java")
                        || normalized.endsWith("/adapter/out/acl/BranchHeadAclAdapter.java")
                        // Task 2.123. Merging writes to the target branch, so it answers to the
                        // repository context's write gate. Delegating to validateCanCommit keeps the
                        // rule in one place, including the visibility split from 577c1a0; restating
                        // it here would produce a second copy of a security decision.
                        || normalized.endsWith("/adapter/out/acl/RepositoryWriteAccessAclAdapter.java")
                        || normalized.endsWith("/adapter/out/acl/RepositoryReadAccessAclAdapter.java")
                        || normalized.endsWith("/adapter/out/acl/RepositoryReferenceAclAdapterTest.java")
                        || normalized.endsWith("/adapter/out/acl/BranchHeadAclAdapterTest.java");
                assertThat(foreignImport && !allowlisted)
                        .as("foreign bounded-context import outside ACL allowlist: " + path)
                        .isFalse();
            }
        });

        assertZeroReferences(sources, "io.jgitkins.server.repository.domain.vo.RepositoryId");
        assertZeroReferences(sources, "io.jgitkins.server.repository.domain.aggregate.Repository");
        assertZeroReferences(sources, "io.jgitkins.server.repository.domain.repository.RepositoryRepository");
        assertZeroReferences(sources, "io.jgitkins.server.repository.application.support.RepositoryLookupService");
        assertZeroReferences(sources, "io.jgitkins.server.repository.application.port.out.BranchGitPort");
        assertZeroReferences(sources, "MergeGitPort");

        Path repositoryAcl = MAIN.resolve("io/jgitkins/server/change/review/adapter/out/acl/RepositoryReferenceAclAdapter.java");
        Path branchAcl = MAIN.resolve("io/jgitkins/server/change/review/adapter/out/acl/BranchHeadAclAdapter.java");
        Path movedGit = MAIN.resolve("io/jgitkins/server/change/review/adapter/out/git/MergeGitAdapter.java");
        assertThat(Files.exists(repositoryAcl)).isTrue();
        assertThat(Files.exists(branchAcl)).isTrue();
        assertThat(Files.exists(movedGit)).isTrue();
        assertThat(Files.exists(TEST.resolve("io/jgitkins/server/change/review/adapter/out/git/MergeGitAdapterTest.java"))).isTrue();
        assertThat(Files.exists(PROJECT.resolve("app-server/src/main/java/io/jgitkins/server/common/infrastructure/adapter/git/MergeGitAdapter.java"))).isFalse();
        assertThat(Files.exists(PROJECT.resolve("app-server/src/test/java/io/jgitkins/server/common/infrastructure/adapter/git/MergeGitAdapterTest.java"))).isFalse();

        assertForeignImportCardinality(read(repositoryAcl), List.of(
                "RepositoryLookupService", "RepositoryRepository", "RepositoryNamespaceResolver",
                "io.jgitkins.server.repository.domain.aggregate.Repository", "io.jgitkins.server.repository.domain.vo.RepositoryId"));
        assertForeignImportCardinality(read(branchAcl), List.of(
                "io.jgitkins.server.repository.application.port.out.BranchGitPort",
                "GitBranchRefMissingException"));

        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/domain/model/vo/ReviewRepositoryId.java"))).isTrue();
        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/application/port/out/RepositoryReferencePort.java"))).isTrue();
        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/application/port/out/BranchHeadPort.java"))).isTrue();
        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/application/port/out/MergePort.java"))).isTrue();
        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/adapter/in/rest/PullRequestManagementController.java"))).isTrue();
        assertThat(Files.exists(MAIN.resolve("io/jgitkins/server/change/review/adapter/in/rest/MergeController.java"))).isTrue();
    }

    private void assertZeroReferences(Map<Path, String> sources, String token) {
        sources.forEach((path, source) -> {
            String normalized = path.toString().replace('\\', '/');
            boolean scoped = normalized.contains("/change/review/domain/") || normalized.contains("/change/review/application/");
            if (token.equals("MergeGitPort")) scoped = normalized.contains("/change/review/");
            if (scoped && !path.toAbsolutePath().normalize().equals(THIS.toAbsolutePath().normalize())) {
                assertThat(source).as(path.toString()).doesNotContain(token);
            }
        });
    }

    private void assertForeignImportCardinality(String source, List<String> tokens) {
        for (String token : tokens) {
            long count = source.lines().filter(line -> line.startsWith("import ") && line.contains(token)).count();
            assertThat(count).as(token).isEqualTo(1);
        }
    }

    private List<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) return List.of();
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private String read(Path path) {
        try { return Files.readString(path); }
        catch (IOException e) { throw new AssertionError(e); }
    }
}
