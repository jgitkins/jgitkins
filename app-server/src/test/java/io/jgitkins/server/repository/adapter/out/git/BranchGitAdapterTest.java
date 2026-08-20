package io.jgitkins.server.repository.adapter.out.git;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.repository.infrastructure.support.RepositoryResolver;
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.internal.BranchCreationContext;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefAlreadyExistsException;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import io.jgitkins.server.repository.application.port.out.exception.GitSourceBranchRefMissingException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BranchGitAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void createBranch_throwsGitSourceBranchRefMissingWhenSourceRefDoesNotExist() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        BranchGitAdapter branchGitAdapter = new BranchGitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "demo");

        assertThatThrownBy(() -> branchGitAdapter.createBranch(context("feature", "missing")))
                .isInstanceOf(GitSourceBranchRefMissingException.class);
    }

    @Test
    void createBranch_throwsGitBranchRefAlreadyExistsWhenTargetRefExists() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        RepositoryGitCommitAdapter commitAdapter = new RepositoryGitCommitAdapter(repositoryResolver);
        BranchGitAdapter branchGitAdapter = new BranchGitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "demo");
        commitAdapter.commit("team", "demo", "main", "initial", "tester", "tester@example.com", files());

        assertThatThrownBy(() -> branchGitAdapter.createBranch(context("main", "main")))
                .isInstanceOf(GitBranchRefAlreadyExistsException.class);
    }

    @Test
    void deleteBranch_throwsGitBranchRefMissingWhenTargetRefDoesNotExist() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        BranchGitAdapter branchGitAdapter = new BranchGitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "demo");

        assertThatThrownBy(() -> branchGitAdapter.deleteBranch("team", "demo", "missing"))
                .isInstanceOf(GitBranchRefMissingException.class);
    }

    @Test
    void getHeadCommitHash_throwsGitBranchRefMissingWhenTargetRefDoesNotExist() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        BranchGitAdapter branchGitAdapter = new BranchGitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "demo");

        assertThatThrownBy(() -> branchGitAdapter.getHeadCommitHash("team", "demo", "missing"))
                .isInstanceOf(GitBranchRefMissingException.class);
    }

    private BranchCreationContext context(String branchName, String sourceBranch) {
        return BranchCreationContext.of(
                new BranchCreateCommand(1L, branchName, sourceBranch, true),
                "team",
                repository(),
                sourceBranch);
    }

    private Repository repository() {
        return Repository.create(
                        null,
                        null,
                        RepositoryName.from("demo"),
                        RepositoryPath.from("demo"),
                        BranchName.of("main"),
                        RepositoryVisibility.PRIVATE,
                        null,
                        "/team/demo.git",
                        null,
                        false)
                .withIdentity(RepositoryId.of(1L), null, null);
    }

    private List<CommitFile> files() {
        return List.of(CommitFile.builder()
                .path("README.md")
                .content("# demo\n".getBytes(StandardCharsets.UTF_8))
                .build());
    }
}
