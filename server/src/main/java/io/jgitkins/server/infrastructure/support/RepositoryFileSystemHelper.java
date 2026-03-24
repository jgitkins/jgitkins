package io.jgitkins.server.infrastructure.support;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;

@UtilityClass
public class RepositoryFileSystemHelper {

    // TODO: refactor
    public void createRepositoryDir(File gitDir) {
        if (gitDir.exists()) {
            throw new ApplicationException(ApplicationErrorCode.REPOSITORY_ALREADY_EXISTS, "Repository already exists: " + gitDir.getAbsolutePath());
        }
        if (!gitDir.mkdirs() && !gitDir.exists()) {
            throw new InfrastructureException(InfrastructureErrorCode.REPOSITORY_CREATE_FAILED, "Failed to create directories: " + gitDir.getAbsolutePath());
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
