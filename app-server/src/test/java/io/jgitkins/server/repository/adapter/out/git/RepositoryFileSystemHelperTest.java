package io.jgitkins.server.repository.adapter.out.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.common.infrastructure.exception.FileSystemAccessFailedException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What creating a bare repository's directory does when the directory is already there.
 *
 * <p>Written because moving this class out of {@code repository/infrastructure} changed the answer,
 * and nothing asserted either the old one or the new one -- {@code RepositoryAlreadyExistsException}
 * appeared in no test in the module. The old behaviour reported an existing directory as that
 * application exception, which the layer rule for {@code adapter/out/git} forbids and which the old
 * location was hiding.
 *
 * <p>The distinction being pinned is not about which exception type reads better. A repository name
 * that is taken is decided from the database, before this code runs. A directory here with no record
 * behind it means the two stores disagree, and reporting that as a name conflict tells the caller to
 * pick another name for a problem another name will not fix.
 */
class RepositoryFileSystemHelperTest {

    @Test
    void refusesADirectoryThatAlreadyExists(@TempDir Path root) throws IOException {
        File gitDir = root.resolve("taken.git").toFile();
        Files.createDirectory(gitDir.toPath());

        assertThatThrownBy(() -> RepositoryFileSystemHelper.createRepositoryDir(gitDir))
                .isInstanceOf(FileSystemAccessFailedException.class)
                .hasMessageContaining("no repository recorded")
                .hasMessageContaining(gitDir.getAbsolutePath());
    }

    @Test
    void createsTheDirectoryAndItsMissingParents(@TempDir Path root) {
        File gitDir = root.resolve("owner").resolve("nested").resolve("fresh.git").toFile();

        RepositoryFileSystemHelper.createRepositoryDir(gitDir);

        assertThat(gitDir)
                .as("the namespace directory does not exist yet on a first repository, so mkdirs and "
                        + "not mkdir is load-bearing")
                .isDirectory();
    }

    @Test
    void deletingWhatIsNotThereIsNotAnError(@TempDir Path root) throws IOException {
        // Repository deletion calls this after removing the row, so a retry of a partly-failed delete
        // arrives with the directory already gone.
        RepositoryFileSystemHelper.deleteRecursively(root.resolve("never-existed").toFile());
        RepositoryFileSystemHelper.deleteRecursively(null);
    }

    @Test
    void deletesADirectoryTreeContentsFirst(@TempDir Path root) throws IOException {
        Path nested = root.resolve("repo.git").resolve("objects").resolve("pack");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("pack-1.pack"), "x");

        RepositoryFileSystemHelper.deleteRecursively(root.resolve("repo.git").toFile());

        assertThat(root.resolve("repo.git"))
                .as("File#delete refuses a non-empty directory, so a shallow delete would leave the "
                        + "repository on disk and report success")
                .doesNotExist();
    }
}
