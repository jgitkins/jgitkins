package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.FileEntry;

import java.util.List;

public interface FileLoadUseCase {
    List<FileEntry> getAllFiles(String namespace, String repoName, String reference);
}
