package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.FileEntry;

import java.io.IOException;
import java.util.List;

public interface FileTreeLoadUseCase {
    List<FileEntry> getTree(String namespace, String repoName, String branch, String directory);
}
