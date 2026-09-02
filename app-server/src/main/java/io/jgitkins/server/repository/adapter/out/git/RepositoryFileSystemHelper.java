package io.jgitkins.server.repository.adapter.out.git;

import io.jgitkins.server.common.infrastructure.exception.FileSystemAccessFailedException;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;

/**
 * Bare-repository filesystem operations for the git adapters.
 *
 * <p>Was {@code repository/infrastructure/support}. Moving it beside its only callers made it subject
 * to the rule that {@code repository/adapter/out/git} may not import
 * {@code repository.application.exception} -- and it was violating it: a directory that already
 * existed was reported as {@code RepositoryAlreadyExistsException}, an application exception. The
 * adapter that calls this never imported that type, so the old location was laundering exactly the
 * dependency the rule forbids.
 *
 * <p>The file's own TODO asked whether these two cases should be abstracted or stay detailed. Moving
 * it forced an answer, and the answer changes an HTTP status. "This name is taken" is decided in the
 * application layer, from the database, by {@code RepositoryValidator#validateRepositoryNameUnique}.
 * By the time control reaches here that check has passed, so a directory on disk means the database
 * and the filesystem disagree -- a bare repository with no record of it. That is not a name conflict
 * the caller can fix by choosing another name, which is what a 409 tells them; it is corrupted state,
 * and it now reports as one.
 */
@UtilityClass
public class RepositoryFileSystemHelper {

    public void createRepositoryDir(File gitDir) {
        if (gitDir.exists()) {
            throw new FileSystemAccessFailedException(
                    "Repository directory already exists with no repository recorded for it: "
                            + gitDir.getAbsolutePath());
        }
        if (!gitDir.mkdirs() && !gitDir.exists()) {
            throw new FileSystemAccessFailedException("Failed to create directories: " + gitDir.getAbsolutePath());
        }
    }

    public void initializeBareRepository(Repository repo) throws IOException {
        repo.create(true);
        repo.getConfig().setBoolean("http", null, "receivepack", true);
        repo.getConfig().save();
    }

    public void deleteRecursively(File target) throws IOException {
        if (target == null || !target.exists()) {
            return;
        }
        File[] contents = target.listFiles();
        if (contents != null) {
            for (File child : contents) {
                deleteRecursively(child);
            }
        }
        if (!target.delete()) {
            throw new IOException("Failed to delete " + target.getAbsolutePath());
        }
    }
}
