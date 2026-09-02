package io.jgitkins.server.change.review.adapter.out.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.change.review.application.contract.MergeRequest;
import io.jgitkins.server.change.review.application.contract.MergeResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MergeGitAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void previewMergeability_marksFastForwardPossible_whenSourceIsAheadOfTarget() throws Exception {
        createBareRepository(Scenario.FAST_FORWARD);
        MergeGitAdapter adapter = new MergeGitAdapter(tempDir.toString());

        MergeResult result = adapter.previewMergeability("team", "demo", "feature", "main");

        assertThat(result.getStatus()).isEqualTo(MergeResult.Status.MERGEABLE);
        assertThat(result.getFastForwardPossible()).isTrue();
        assertThat(result.getMergeCommitRequired()).isFalse();
    }

    @Test
    void previewMergeability_marksMergeCommitRequired_whenBranchesDiverged() throws Exception {
        createBareRepository(Scenario.DIVERGED);
        MergeGitAdapter adapter = new MergeGitAdapter(tempDir.toString());

        MergeResult result = adapter.previewMergeability("team", "demo", "feature", "main");

        assertThat(result.getStatus()).isEqualTo(MergeResult.Status.MERGEABLE);
        assertThat(result.getFastForwardPossible()).isFalse();
        assertThat(result.getMergeCommitRequired()).isTrue();
    }

    @Test
    void mergeCreatesCommitAndUpdatesTargetRef() throws Exception {
        createBareRepository(Scenario.DIVERGED);
        MergeGitAdapter adapter = new MergeGitAdapter(tempDir.toString());
        MergeRequest request = new MergeRequest("feature", "main", "merge feature", "tester", "tester@example.com");

        MergeResult result = adapter.merge("team", "demo", request);

        assertThat(result.getStatus()).isEqualTo(MergeResult.Status.MERGED);
        assertThat(result.getNewCommitId()).isNotBlank();
        assertThat(result.getResultTreeId()).isNotBlank();
        assertThat(adapter.previewMergeability("team", "demo", "feature", "main").getStatus())
                .isEqualTo(MergeResult.Status.ALREADY_UP_TO_DATE);
    }

    @Test
    void mergeReportsMissingBranch() throws Exception {
        createBareRepository(Scenario.FAST_FORWARD);
        MergeGitAdapter adapter = new MergeGitAdapter(tempDir.toString());
        MergeRequest request = new MergeRequest("missing", "main", null, "tester", "tester@example.com");

        assertThatThrownBy(() -> adapter.merge("team", "demo", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source branch not found: missing");
    }

    @Test
    void mergePropagatesRepositoryIoFailure() {
        MergeGitAdapter adapter = new MergeGitAdapter(tempDir.toString());
        MergeRequest request = new MergeRequest("feature", "main", null, "tester", "tester@example.com");

        assertThatThrownBy(() -> adapter.merge("missing", "demo", request))
                .isInstanceOf(IOException.class);
    }

    private void createBareRepository(Scenario scenario) throws Exception {
        Path workingDirectory = tempDir.resolve("working-" + scenario.name().toLowerCase());
        Path bareDirectory = tempDir.resolve("team").resolve("demo.git");
        Files.createDirectories(workingDirectory);
        Files.createDirectories(bareDirectory.getParent());

        try (Git git = Git.init().setInitialBranch("main").setDirectory(workingDirectory.toFile()).call()) {
            commit(git, workingDirectory, "README.md", "# demo\n", "initial commit");

            git.checkout().setCreateBranch(true).setName("feature").call();
            commit(git, workingDirectory, "feature.txt", "feature\n", "feature commit");

            if (scenario == Scenario.DIVERGED) {
                git.checkout().setName("main").call();
                commit(git, workingDirectory, "main.txt", "main\n", "main commit");
            }

            try (Git ignored = Git.cloneRepository()
                    .setURI(workingDirectory.toUri().toString())
                    .setBare(true)
                    .setDirectory(bareDirectory.toFile())
                    .call()) {
                // The bare clone is the fixture consumed by MergeGitAdapter.
            }
        } finally {
            deleteRecursively(workingDirectory);
        }
    }

    private void commit(Git git, Path workingDirectory, String fileName, String content, String message) throws Exception {
        Files.writeString(workingDirectory.resolve(fileName), content);
        git.add().addFilepattern(fileName).call();
        git.commit()
                .setMessage(message)
                .setAuthor("tester", "tester@example.com")
                .setCommitter("tester", "tester@example.com")
                .call();
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

    private enum Scenario {
        FAST_FORWARD,
        DIVERGED
    }
}
