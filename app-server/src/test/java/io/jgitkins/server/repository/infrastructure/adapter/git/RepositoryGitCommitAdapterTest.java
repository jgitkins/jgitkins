package io.jgitkins.server.repository.infrastructure.adapter.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.repository.infrastructure.support.RepositoryResolver;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import io.jgitkins.server.repository.infrastructure.adapter.git.RepositoryGitFileAdapter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryGitCommitAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void commit_createsInitialCommitAndBranchForBareRepository() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        RepositoryGitCommitAdapter commitAdapter = new RepositoryGitCommitAdapter(repositoryResolver);
        RepositoryGitFileAdapter fileAdapter = new RepositoryGitFileAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "demo");

        commitAdapter.commit(
                "team",
                "demo",
                "main",
                "initial commit",
                "tester",
                "tester@example.com",
                List.of(CommitFile.builder()
                        .path("README.md")
                        .content("# demo\n".getBytes(StandardCharsets.UTF_8))
                        .build()));

        List<CommitHistory> histories = commitAdapter.listCommitHistory("team", "demo", "main");

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getMessage()).isEqualTo("initial commit");
        assertThat(fileAdapter.listTree("team", "demo", "main", ""))
                .extracting(io.jgitkins.server.repository.application.contract.result.FileEntry::getPath)
                .contains("README.md");
    }

    @Test
    void listCommitHistory_returnsEmptyListWhenBranchRefDoesNotExist() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        RepositoryGitCommitAdapter commitAdapter = new RepositoryGitCommitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "empty-repo");

        List<CommitHistory> histories = commitAdapter.listCommitHistory("team", "empty-repo", "main");

        assertThat(histories).isEmpty();
    }

    @Test
    void loadCommit_throwsGitCommitObjectMissingWhenCommitDoesNotExist() {
        RepositoryResolver repositoryResolver = new RepositoryResolver(tempDir.toString());
        RepositoryGitAdapter repositoryGitAdapter = new RepositoryGitAdapter(repositoryResolver);
        RepositoryGitCommitAdapter commitAdapter = new RepositoryGitCommitAdapter(repositoryResolver);

        repositoryGitAdapter.initialize("team", "empty-repo");

        assertThatThrownBy(() -> commitAdapter.loadCommit("team", "empty-repo", "deadbeef"))
                .isInstanceOf(GitCommitObjectMissingException.class);
    }
}
