package io.jgitkins.server.repository.adapter.out.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.repository.infrastructure.support.RepositoryResolver;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import io.jgitkins.server.repository.adapter.out.git.RepositoryGitFileAdapter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
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

    // --- task 2.122: the commit tree must overlay the parent, not replace it -------------------

    private static CommitFile file(String path, String body) {
        return CommitFile.builder().path(path).content(body.getBytes(StandardCharsets.UTF_8)).build();
    }

    private record Fixture(RepositoryGitCommitAdapter commit, RepositoryGitFileAdapter files,
            RepositoryResolver resolver) {
    }

    private Fixture repositoryWith(String namespace, String name, List<CommitFile> initial) {
        RepositoryResolver resolver = new RepositoryResolver(tempDir.toString());
        new RepositoryGitAdapter(resolver).initialize(namespace, name);
        RepositoryGitCommitAdapter commit = new RepositoryGitCommitAdapter(resolver);
        if (!initial.isEmpty()) {
            commit.commit(namespace, name, "main", "seed", "tester", "t@example.com", initial);
        }
        return new Fixture(commit, new RepositoryGitFileAdapter(resolver), resolver);
    }

    private List<String> pathsOn(Fixture f, String namespace, String name) {
        return f.files().listTree(namespace, name, "main", "").stream()
                .map(io.jgitkins.server.repository.application.contract.result.FileEntry::getPath)
                .toList();
    }

    @Test
    void commit_preservesTheOtherFilesAlreadyOnTheBranch() {
        Fixture f = repositoryWith("team", "overlay",
                List.of(file("a.txt", "A"), file("b.txt", "B")));

        f.commit().commit("team", "overlay", "main", "add c", "tester", "t@example.com",
                List.of(file("c.txt", "C")));

        // Before task 2.122 this returned ["c.txt"]: the tree was built from an empty DirCache and
        // held only the uploaded file, so a.txt and b.txt disappeared from HEAD.
        assertThat(pathsOn(f, "team", "overlay")).containsExactlyInAnyOrder("a.txt", "b.txt", "c.txt");
    }


    /** Reads a blob straight from JGit: the file adapter exposes listing, not content. */
    private String blobAt(Fixture f, String namespace, String name, String path) throws IOException {
        try (Repository repository = f.resolver().openBareRepository(namespace, name);
                org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            org.eclipse.jgit.revwalk.RevCommit head =
                    walk.parseCommit(repository.findRef("refs/heads/main").getObjectId());
            try (org.eclipse.jgit.treewalk.TreeWalk tw =
                    org.eclipse.jgit.treewalk.TreeWalk.forPath(repository, path, head.getTree())) {
                return new String(repository.open(tw.getObjectId(0)).getBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    void commit_replacesAFileCommittedAtTheSamePath() throws IOException {
        Fixture f = repositoryWith("team", "replace", List.of(file("a.txt", "old"), file("b.txt", "B")));

        f.commit().commit("team", "replace", "main", "update a", "tester", "t@example.com",
                List.of(file("a.txt", "new")));

        // PathEdit replaces rather than appending. builder.add would have left two entries for a.txt.
        assertThat(pathsOn(f, "team", "replace")).containsExactlyInAnyOrder("a.txt", "b.txt");
        assertThat(blobAt(f, "team", "replace", "a.txt")).isEqualTo("new");
    }

    @Test
    void commit_onABranchWithNoParent_writesOnlyTheCommittedFile() {
        Fixture f = repositoryWith("team", "first", List.of());

        f.commit().commit("team", "first", "main", "initial", "tester", "t@example.com",
                List.of(file("README.md", "# first")));

        // Regression guard for the overlay change: with no parent there is nothing to overlay onto.
        assertThat(pathsOn(f, "team", "first")).containsExactly("README.md");
    }

    @Test
    void commit_preservesFilesInOtherDirectories() {
        Fixture f = repositoryWith("team", "dirs",
                List.of(file("src/main.java", "code"), file("docs/readme.md", "docs")));

        f.commit().commit("team", "dirs", "main", "add root file", "tester", "t@example.com",
                List.of(file("LICENSE", "MIT")));

        assertThat(f.files().listTree("team", "dirs", "main", "src"))
                .extracting(io.jgitkins.server.repository.application.contract.result.FileEntry::getPath)
                .contains("src/main.java");
        assertThat(f.files().listTree("team", "dirs", "main", "docs"))
                .extracting(io.jgitkins.server.repository.application.contract.result.FileEntry::getPath)
                .contains("docs/readme.md");
    }

    // --- task 2.122 / E3: the ref CAS must use the parent the tree was built from ---------------

    @Test
    void updateBranchReference_refusesWhenTheHeadMovedSinceTheTreeWasBuilt() throws IOException {
        Fixture f = repositoryWith("team", "race", List.of(file("a.txt", "A")));
        RepositoryGitCommitAdapter adapter = f.commit();

        try (Repository repository = f.resolver().openBareRepository("team", "race")) {
            ObjectId actualHead = repository.findRef("refs/heads/main").getObjectId();
            ObjectId staleParent = ObjectId.fromString("0123456789012345678901234567890123456789");

            // The commit id does not matter; what is asserted is that the update is rejected when the
            // expected parent is not what the ref currently holds. updateBranchReference used to
            // re-read the head here, so the comparison was always against itself and always passed --
            // a commit that landed in between was silently overwritten.
            assertThatThrownBy(() ->
                    adapter.updateBranchReference(repository, "main", actualHead, staleParent))
                    .isInstanceOf(IOException.class);
        }
    }

    @Test
    void updateBranchReference_refusesToCreateABranchThatAlreadyExists() throws IOException {
        Fixture f = repositoryWith("team", "exists", List.of(file("a.txt", "A")));
        RepositoryGitCommitAdapter adapter = f.commit();

        try (Repository repository = f.resolver().openBareRepository("team", "exists")) {
            ObjectId head = repository.findRef("refs/heads/main").getObjectId();

            // null parent means "the branch did not exist when the tree was built", which the CAS
            // expresses as zeroId. If the branch exists now, someone created it in between.
            assertThatThrownBy(() -> adapter.updateBranchReference(repository, "main", head, null))
                    .isInstanceOf(IOException.class);
        }
    }
}
