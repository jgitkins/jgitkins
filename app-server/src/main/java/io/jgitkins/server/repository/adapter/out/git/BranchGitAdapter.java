package io.jgitkins.server.repository.adapter.out.git;

import java.io.IOException;
import io.jgitkins.server.common.infrastructure.exception.BranchCreateFailedException;
import io.jgitkins.server.common.infrastructure.exception.BranchDeleteFailedException;
import io.jgitkins.server.common.infrastructure.exception.HeadReferenceResolveFailedException;
import io.jgitkins.server.repository.infrastructure.support.RepositoryResolver;
import io.jgitkins.server.repository.application.contract.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefAlreadyExistsException;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import io.jgitkins.server.repository.application.port.out.exception.GitSourceBranchRefMissingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchGitAdapter implements BranchGitPort {

    private final RepositoryResolver repositoryResolver;

    @Override
    public void createBranch(BranchCreationContext context) {
        String namespace = context.namespace();
        String repoName = context.repositoryName();
        String sourceBranch = context.sourceBranch();
        String branchName = context.branchName();

        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            try (Git git = new Git(repo)) {
                if (repo.resolve(sourceBranch) == null) {
                    throw new GitSourceBranchRefMissingException(sourceBranch);
                }

                if (repo.resolve(branchName) != null) {
                    throw new GitBranchRefAlreadyExistsException(branchName);
                }

                git.branchCreate()
                        .setName(branchName)
                        .setStartPoint(sourceBranch)
                        .call();
            }
        } catch (RefNotFoundException e) {
            throw new GitSourceBranchRefMissingException(sourceBranch, e);
        } catch (GitAPIException | IOException e) {
            throw new BranchCreateFailedException(
                    "Failed to create branch: " + branchName, e);
        }
    }

    @Override
    public void deleteBranch(String namespace, String repoName, String branchName) {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            try (Git git = new Git(repo)) {
                if (repo.resolve(branchName) == null) {
                    throw new GitBranchRefMissingException(branchName);
                }

                git.branchDelete()
                        .setBranchNames(branchName)
                        .setForce(true)
                        .call();
            }
        } catch (GitAPIException | IOException e) {
            throw new BranchDeleteFailedException(
                    "Failed to delete branch: " + branchName, e);
        }
    }

    @Override
    public String getHeadCommitHash(String namespace, String repoName, String branchName) {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            ObjectId objectId = repo.resolve(branchName);
            if (objectId == null) {
                throw new GitBranchRefMissingException(branchName);
            }
            return objectId.name();
        } catch (IOException e) {
            throw new HeadReferenceResolveFailedException(
                    "Failed to resolve branch head: " + branchName, e);
        }
    }
}
