package io.jgitkins.server.infrastructure.support;

import io.jgitkins.server.application.exception.RepositoryAlreadyExistsException;
import io.jgitkins.server.infrastructure.exception.FileSystemAccessFailedException;
import lombok.experimental.UtilityClass;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;

@UtilityClass
public class RepositoryFileSystemHelper {

    // TODO: refactor
    // TODO: 음.. 두케이스 예외를 던질때 약간 추상화해서 RepositoryCreationFailedException 이라고 던져야될지 아니면 기존처럼 상세히 던져야될지 검토필요
    public void createRepositoryDir(File gitDir) {
        if (gitDir.exists()) {
            throw new RepositoryAlreadyExistsException("Repository already exists: " + gitDir.getAbsolutePath());
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
