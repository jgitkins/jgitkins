package io.jgitkins.server.infrastructure.adapter.git;

import io.jgitkins.server.application.common.GitConstants;
import io.jgitkins.server.application.port.out.RepositoryGitPort;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.support.RepositoryFileSystemHelper;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
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
            throw new InfrastructureException(InfrastructureErrorCode.REPOSITORY_CREATE_FAILED, "Repository creation failed: " + gitDir.getAbsolutePath(), e);
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
            throw new InfrastructureException(InfrastructureErrorCode.REPOSITORY_DELETE_FAILED, "Failed to delete repository directory: " + gitDir.getAbsolutePath(), e);
        }
    }

    @Override
    public void updateHeadReference(String namespace, String repoName, String branch) {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            String mainRef = GitConstants.REFS_HEADS_PREFIX + branch;
            repo.updateRef(Constants.HEAD, true)
                    .link(mainRef);
        } catch (IOException e) {
            throw new InfrastructureException(InfrastructureErrorCode.HEAD_POINT_FAILED, String.format("Failed to link HEAD for repo %s/%s", namespace, repoName), e);
        }
    }

}
