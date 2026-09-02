package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.FileEntry;

import java.io.IOException;
import java.util.List;

public interface FileTreeLoadUseCase {
    /** @param requesterUserId nullable: a public repository is readable anonymously. */
    List<FileEntry> getTree(String namespace, String repoName, String branch, String directory,
            Long requesterUserId);
}
