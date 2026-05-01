package io.jgitkins.server.repository.infrastructure.adapter.git;

import io.jgitkins.server.repository.application.contract.command.BranchCreationContext;
import io.jgitkins.server.repository.application.exception.BranchAlreadyExistsException;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.SourceBranchNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.infrastructure.exception.BranchCreateFailedException;
import io.jgitkins.server.infrastructure.exception.BranchDeleteFailedException;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchGitAdapter implements BranchGitPort {

    private final RepositoryResolver repositoryResolver;

    @Override
    public void createBranch(BranchCreationContext context) {
        String namespace = context.getNamespace();
        String repoName = context.getRepositoryName();
        String sourceBranch = context.getSourceBranch();
        String branchName = context.getBranchName();

        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            try (Git git = new Git(repo)) {
                // TODO: refactor 수정필요 Adapter에서 ApplicationException 모르기
                if (repo.resolve(sourceBranch) == null) {
                    throw new SourceBranchNotFoundException(sourceBranch);
                }

                if (repo.resolve(branchName) != null) {
                    throw new BranchAlreadyExistsException(branchName);
                }

                git.branchCreate()
                        .setName(branchName)
                        .setStartPoint(sourceBranch)
                        .call();
            }
        } catch (RefNotFoundException e) {
            throw new BranchCreateFailedException(
                    "Failed to create branch - Ref not found: " + sourceBranch, e);
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
                    // TODO: refactor 수정필요 Adapter에서 ApplicationException 모르기
                    throw new BranchNotFoundException(branchName);
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
    public String getHeadCommitHash(String namespace, String repoName, String branchName) throws IOException {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            ObjectId objectId = repo.resolve(branchName);
            if (objectId == null) {
                throw new BranchNotFoundException(branchName);
            }
            return objectId.name();
        }
    }
}
