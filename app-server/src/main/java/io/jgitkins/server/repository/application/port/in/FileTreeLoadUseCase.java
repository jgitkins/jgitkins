package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.FileEntry;

import java.io.IOException;
import java.util.List;

public interface FileTreeLoadUseCase {
    List<FileEntry> getTree(String namespace, String repoName, String branch, String directory);
}
