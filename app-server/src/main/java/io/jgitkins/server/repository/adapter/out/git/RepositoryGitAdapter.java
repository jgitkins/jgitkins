package io.jgitkins.server.repository.adapter.out.git;

import io.jgitkins.server.shared.common.GitConstants;
import io.jgitkins.server.repository.application.port.out.RepositoryGitPort;
import io.jgitkins.server.common.infrastructure.exception.HeadReferenceUpdateFailedException;
import io.jgitkins.server.common.infrastructure.exception.RepositoryCreateFailedException;
import io.jgitkins.server.common.infrastructure.exception.RepositoryDeleteFailedException;
import io.jgitkins.server.repository.adapter.out.git.RepositoryFileSystemHelper;
import io.jgitkins.server.repository.adapter.out.git.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryGitAdapter implements RepositoryGitPort {

    private final RepositoryResolver repositoryResolver;

    @Override
    public void initialize(String namespace, String repoName) {
        File gitDir = repositoryResolver.resolveGitDir(namespace, repoName);
        long startedAt = System.nanoTime();
        log.info("Repository git create started. namespace={}, repoName={}", namespace, repoName);
        try {
            RepositoryFileSystemHelper.createRepositoryDir(gitDir);
            try (Repository repo = repositoryResolver.openBareRepository(gitDir)) {
                RepositoryFileSystemHelper.initializeBareRepository(repo);
                long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                log.info("Repository git create completed. namespace={}, repoName={}, durationMs={}", namespace, repoName, durationMs);
            }
        } catch (IOException e) {
            log.error("Repository git create failed. namespace={}, repoName={}", namespace, repoName, e);
            throw new RepositoryCreateFailedException("Repository creation failed: " + gitDir.getAbsolutePath(), e);
        }
    }

    @Override
    public void deleteRepository(String namespace, String repoName) {
        File gitDir = repositoryResolver.resolveGitDir(namespace, repoName);
        if (!gitDir.exists()) {
            log.info("Skip repository delete. repo not found path={}, namespace={}", gitDir.getAbsolutePath(), namespace);
            return;
        }
        try {
            RepositoryFileSystemHelper.deleteRecursively(gitDir);
            File parent = gitDir.getParentFile();
            if (parent != null && parent.isDirectory()) {
                File[] siblings = parent.listFiles();
                if (siblings != null && siblings.length == 0) {
                    parent.delete();
                }
            }
        } catch (IOException e) {
            throw new RepositoryDeleteFailedException("Failed to delete repository directory: " + gitDir.getAbsolutePath(), e);
        }
    }

    @Override
    public void updateHeadReference(String namespace, String repoName, String branch) {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            String mainRef = GitConstants.REFS_HEADS_PREFIX + branch;
            repo.updateRef(Constants.HEAD, true)
                    .link(mainRef);
        } catch (IOException e) {
            throw new HeadReferenceUpdateFailedException(String.format("Failed to link HEAD for repo %s/%s", namespace, repoName), e);
        }
    }

}
