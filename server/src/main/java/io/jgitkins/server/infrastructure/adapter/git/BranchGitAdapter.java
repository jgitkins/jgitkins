package io.jgitkins.server.infrastructure.adapter.git;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.command.BranchCreationContext;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.BranchGitPort;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
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
                    throw new ApplicationException(ApplicationErrorCode.SOURCE_BRANCH_NOT_FOUND, "Source branch not found: " + sourceBranch);
                }

                if (repo.resolve(branchName) != null) {
                    throw new ApplicationException(ApplicationErrorCode.BRANCH_ALREADY_EXISTS, "Branch already exists: " + branchName);
                }

                git.branchCreate()
                        .setName(branchName)
                        .setStartPoint(sourceBranch)
                        .call();
            }
        } catch (RefNotFoundException e) {
            throw new InfrastructureException(InfrastructureErrorCode.BRANCH_CREATE_FAILED,
                    "Failed to create branch - Ref not found: " + sourceBranch, e);
        } catch (GitAPIException | IOException e) {
            throw new InfrastructureException(InfrastructureErrorCode.BRANCH_CREATE_FAILED,
                    "Failed to create branch: " + branchName, e);
        }
    }

    @Override
    public void deleteBranch(String namespace, String repoName, String branchName) {
        try (Repository repo = repositoryResolver.openBareRepository(namespace, repoName)) {
            try (Git git = new Git(repo)) {
                if (repo.resolve(branchName) == null) {
                    // TODO: refactor 수정필요 Adapter에서 ApplicationException 모르기
                    throw new ApplicationException(ApplicationErrorCode.BRANCH_NOT_FOUND,
                            "Branch not found: " + branchName);
                }

                git.branchDelete()
                        .setBranchNames(branchName)
                        .setForce(true)
                        .call();
            }
        } catch (GitAPIException | IOException e) {
            throw new InfrastructureException(InfrastructureErrorCode.BRANCH_DELETE_FAILED,
                    "Failed to delete branch: " + branchName, e);
        }
    }
}
