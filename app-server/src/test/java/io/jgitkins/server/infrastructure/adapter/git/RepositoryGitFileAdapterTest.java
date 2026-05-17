package io.jgitkins.server.infrastructure.adapter.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryGitFileAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void listTree_returnsTreeTypeForDirectories() throws Exception {
        createBareRepositoryWithDirectoryTree();
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitFileAdapter adapter = new RepositoryGitFileAdapter(repositoryResolver);

        List<FileEntry> entries = adapter.listTree("team", "demo", "main", "");

        assertThat(entries)
                .extracting(FileEntry::getName, FileEntry::getType, FileEntry::isDirectory)
                .contains(
                        tuple("src", "tree", true),
                        tuple("README.md", "blob", false)
                );
    }

    private void createBareRepositoryWithDirectoryTree() throws Exception {
        Path workingDirectory = tempDir.resolve("working");
        Path bareDirectory = tempDir.resolve("team").resolve("demo.git");
        Files.createDirectories(workingDirectory);
        Files.createDirectories(bareDirectory.getParent());

        try (Git git = Git.init().setInitialBranch("main").setDirectory(workingDirectory.toFile()).call()) {
            Files.createDirectories(workingDirectory.resolve("src"));
            Files.writeString(workingDirectory.resolve("src/App.java"), "class App {}");
            Files.writeString(workingDirectory.resolve("README.md"), "# demo");
            git.add().addFilepattern(".").call();
            git.commit()
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
            // bare repository creation only
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
