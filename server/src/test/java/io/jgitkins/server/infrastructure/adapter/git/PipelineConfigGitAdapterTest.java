package io.jgitkins.server.infrastructure.adapter.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineConfigGitAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void read_returnsRulesFromDotJgitkinsCiYml() throws Exception {
        RevCommit commit = createBareRepository("""
                on:
                  push:
                    rules:
                      - branches: [main, develop]
                        file: pipelines/main.Jenkinsfile
                      - branches: [release/*]
                        file: pipelines/release.Jenkinsfile
                """);

        PipelineConfigGitAdapter adapter = new PipelineConfigGitAdapter(new RepositoryResolver(tempDir.toString()));

        PipelineConfig config = adapter.read("team", "demo", commit.getName());

        assertThat(config.getRules()).hasSize(2);
        assertThat(config.findRule("main")).isPresent();
        assertThat(config.findRule("main").orElseThrow().getFile()).isEqualTo("pipelines/main.Jenkinsfile");
        assertThat(config.findRule("release/1.0")).isPresent();
        assertThat(config.findRule("release/1.0").orElseThrow().getFile()).isEqualTo("pipelines/release.Jenkinsfile");
    }

    @Test
    void read_returnsEmptyRules_whenConfigFileMissing() throws Exception {
        RevCommit commit = createBareRepository("# no config");

        Path working = tempDir.resolve("working-missing");
        Files.createDirectories(working);

        try (Git git = Git.init().setInitialBranch("main").setDirectory(working.toFile()).call()) {
            Files.writeString(working.resolve("README.md"), "# demo");
            git.add().addFilepattern(".").call();
            RevCommit missingConfigCommit = git.commit()
                    .setMessage("missing config")
                    .setAuthor("tester", "tester@example.com")
                    .setCommitter("tester", "tester@example.com")
                    .call();

            Path bareDirectory = tempDir.resolve("missing").resolve("demo.git");
            Files.createDirectories(bareDirectory.getParent());
            try (Git ignored = Git.cloneRepository()
                    .setURI(working.toUri().toString())
                    .setBare(true)
                    .setDirectory(bareDirectory.toFile())
                    .call()) {
                PipelineConfigGitAdapter adapter = new PipelineConfigGitAdapter(new RepositoryResolver(tempDir.toString()));
                PipelineConfig config = adapter.read("missing", "demo", missingConfigCommit.getName());
                assertThat(config.getRules()).isEmpty();
            }
        } finally {
            deleteRecursively(working);
        }
    }

    @Test
    void read_throwsInfrastructureException_whenYamlMalformed() throws Exception {
        RevCommit commit = createBareRepository("""
                on:
                  push:
                    rules:
                      - branches: [main
                        file: pipelines/main.Jenkinsfile
                """);

        PipelineConfigGitAdapter adapter = new PipelineConfigGitAdapter(new RepositoryResolver(tempDir.toString()));

        assertThatThrownBy(() -> adapter.read("team", "demo", commit.getName()))
                .isInstanceOf(InfrastructureException.class);
    }

    private RevCommit createBareRepository(String ciYamlContent) throws Exception {
        Path workingDirectory = tempDir.resolve("working");
        Path bareDirectory = tempDir.resolve("team").resolve("demo.git");
        Files.createDirectories(workingDirectory);
        Files.createDirectories(bareDirectory.getParent());

        RevCommit commit;
        try (Git git = Git.init().setInitialBranch("main").setDirectory(workingDirectory.toFile()).call()) {
            Files.createDirectories(workingDirectory.resolve(".jgitkins"));
            Files.writeString(workingDirectory.resolve(".jgitkins/ci.yml"), ciYamlContent);
            Files.writeString(workingDirectory.resolve(".jgitkins/pipelines.txt"), "placeholder");
            git.add().addFilepattern(".").call();
            commit = git.commit()
                    .setMessage("initial commit")
                    .setAuthor("tester", "tester@example.com")
                    .setCommitter("tester", "tester@example.com")
                    .call();
        }

        try (Git ignored = Git.cloneRepository()
                .setURI(workingDirectory.toUri().toString())
                .setBare(true)
                .setDirectory(bareDirectory.toFile())
                .call()) {
            return commit;
        } finally {
            deleteRecursively(workingDirectory);
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }
}
