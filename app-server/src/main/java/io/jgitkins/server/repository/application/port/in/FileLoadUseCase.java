package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.FileEntry;

import java.util.List;

public interface FileLoadUseCase {
    /** @param requesterUserId nullable: a public repository is readable anonymously. */
    List<FileEntry> getAllFiles(String namespace, String repoName, String reference, Long requesterUserId);
}
