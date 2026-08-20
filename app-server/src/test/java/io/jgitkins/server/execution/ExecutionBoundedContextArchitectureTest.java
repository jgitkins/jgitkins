package io.jgitkins.server.execution;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ExecutionBoundedContextArchitectureTest {
    private static final Path PROJECT = Files.exists(Path.of("src/main/java/io/jgitkins/server/execution"))
            ? Path.of(".") : Path.of("app-server");
    private static final Path MAIN = PROJECT.resolve("src/main/java/io/jgitkins/server/execution");
    private static final Path TEST = PROJECT.resolve("src/test/java/io/jgitkins/server/execution");
    private static final String[] FOREIGN = {
            "io.jgitkins.server.identity.access.domain",
            "io.jgitkins.server.repository.domain",
            "io.jgitkins.server.collaboration.domain",
            "io.jgitkins.server.repository.application.support.CloneUrlBuilder"
    };

    @Test
    void migratedProductionAndTestRootsContainOnlyTheDocumentedForeignExceptions() throws Exception {
        List<String> hits = sourceFiles(MAIN.resolve("domain"), MAIN.resolve("application"), TEST)
                .flatMap(path -> lines(path).stream().filter(line -> containsForeign(line))
                        .map(line -> path + ":" + line))
                .filter(hit -> !hit.contains("ExecutionBoundedContextArchitectureTest.java")
                        && !hit.contains("RepositoryCloneUrlAclAdapter.java")
                        && !hit.contains("RepositoryCloneUrlAclAdapterTest.java")
                        && !hit.contains("ExecutionAclWiringTest.java")
                        && !hit.contains("PushEventHandleServiceIntegrationTest.java"))
                .toList();
        assertThat(hits).as("unexpected foreign imports in migrated execution roots").isEmpty();

        assertThat(sourceFiles(MAIN.resolve("adapter/out/acl"))
                .flatMap(path -> lines(path).stream()
                        .filter(line -> line.startsWith("import ") && line.contains("CloneUrlBuilder"))).toList())
                .hasSize(1);
        assertThat(sourceFiles(MAIN)
                .filter(path -> lines(path).stream().anyMatch(line -> line.contains("CloneUrlBuilder")))
                .count()).isEqualTo(1);

        assertThat(sourceFiles(TEST.resolve("application/service"))
                .flatMap(path -> lines(path).stream()
                        .filter(line -> line.startsWith("import ") && line.contains("BranchRepository"))).toList())
                .containsExactly("import io.jgitkins.server.repository.domain.repository.BranchRepository;");
    }

    @Test
    void migratedDomainHasNoFrameworkOrInfrastructureImports() throws Exception {
        List<String> forbidden = sourceFiles(MAIN.resolve("domain"))
                .flatMap(path -> lines(path).stream()
                        .filter(line -> line.startsWith("import "))
                        .filter(line -> line.contains("org.springframework")
                                || line.contains("org.mybatis")
                                || line.contains("org.eclipse.jgit")
                                || line.contains("io.jgitkins.server.grpc")
                                || line.contains("infrastructure.persistence")))
                .toList();
        assertThat(forbidden).isEmpty();
    }

    @Test
    void lifecyclePortsExposeOnlyTheirBoundedContextOperations() {
        assertThat(JobRepository.class.getDeclaredMethods()).extracting("name")
                .containsExactlyInAnyOrder("save", "findById", "appendHistoryIfCurrent");
        assertThat(JobDispatchQueryPort.class.getDeclaredMethods()).extracting("name")
                .containsExactly("fetchNextJob");
    }

    @Test
    void organizeIdIsConfinedToProjectionAndProtocolSources() throws Exception {
        List<String> hits = sourceFiles(MAIN.resolve("domain"), MAIN.resolve("application/port"))
                .flatMap(path -> lines(path).stream()
                        .filter(line -> line.contains("organizeId")))
                .toList();
        assertThat(hits).isEmpty();
    }

    private boolean containsForeign(String line) {
        for (String token : FOREIGN) if (line.contains(token)) return true;
        return false;
    }

    private Stream<Path> sourceFiles(Path... roots) throws Exception {
        return Stream.of(roots).filter(Files::exists).flatMap(root -> {
            try { return Files.walk(root); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }).filter(path -> path.toString().endsWith(".java"));
    }

    private List<String> lines(Path path) {
        try { return Files.readAllLines(path); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
