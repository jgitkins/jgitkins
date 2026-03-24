package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.FileEntry;

import java.util.List;

public interface FileGitPort {
    List<FileEntry> listTree(String namespace, String repoName, String branch, String directory);
    List<FileEntry> listAllFiles(String namespace, String repoName, String reference);

    boolean exists(String namespace, String repoName, String commitHash, String filePath);
}
