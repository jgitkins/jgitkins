package io.jgitkins.server.repository.adapter.out.git;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.common.infrastructure.exception.CommitFailedException;
import io.jgitkins.server.common.infrastructure.exception.CommitLoadFailedException;
import io.jgitkins.server.repository.infrastructure.support.RepositoryResolver;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryGitCommitAdapter implements CommitGitPort {

    private final RepositoryResolver repositoryResolver;

    @Override
    public CommitHistory loadCommit(String namespace, String repoName, String commitHash) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            ObjectId commitId = repository.resolve(commitHash);
            if (commitId == null) {
                throw new GitCommitObjectMissingException(commitHash);
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit revCommit = revWalk.parseCommit(commitId);
                return toHistory(revCommit);
            }
        } catch (MissingObjectException | IncorrectObjectTypeException e) {
            throw new GitCommitObjectMissingException(commitHash, e);
        } catch (IOException e) {
            throw new CommitLoadFailedException(
                    "Failed to load commit: " + commitHash, e);
        }
    }

    @Override
    public List<CommitHistory> listCommitHistory(String namespace, String repoName, String branch) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName);
                Git git = new Git(repository)) {
            ObjectId branchHead = resolveBranchHead(repository, branch);
            if (branchHead == null) {
                return List.of();
            }

            Iterable<RevCommit> logs = git.log().add(branchHead).call();

            List<CommitHistory> histories = new ArrayList<>();
            for (RevCommit revCommit : logs) {
                histories.add(toHistory(revCommit));
            }
            return histories;
        } catch (IOException | GitAPIException e) {
            throw new CommitLoadFailedException(
                    "Failed to load commit histories for branch: " + branch, e);
        }
    }

    @Override
    public void commit(String namespace,
            String repoName,
            String branch,
            String message,
            String authorName,
            String authorEmail,
            List<CommitFile> files) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            // The branch head is read ONCE and used for both the tree and the ref compare-and-swap.
            // updateBranchReference used to re-read it, which made the CAS compare a value against
            // itself: a commit that landed in between passed the check and was then overwritten.
            ObjectId parentCommitId = resolveBranchHead(repository, branch);
            ObjectId commitId = createCommit(
                    repository, branch, message, authorName, authorEmail, files, parentCommitId);
            updateBranchReference(repository, branch, commitId, parentCommitId);
        } catch (IOException e) {
            throw new CommitFailedException("Failed to commit changes", e);
        }
    }

    private ObjectId createCommit(Repository repository,
            String branch,
            String message,
            String authorName,
            String authorEmail,
            List<CommitFile> files,
            ObjectId parentCommitId) throws IOException {
        String commitMessage = StringUtils.hasText(message) ? message.trim() : "Initial commit";
        PersonIdent ident = createPersonIdent(authorName, authorEmail);

        try (ObjectInserter inserter = repository.newObjectInserter()) {
            ObjectId treeId = writeTree(repository, inserter, files, parentCommitId);

            CommitBuilder commitBuilder = new CommitBuilder();
            commitBuilder.setTreeId(treeId);
            commitBuilder.setMessage(commitMessage);
            commitBuilder.setAuthor(ident);
            commitBuilder.setCommitter(ident);
            if (parentCommitId != null) {
                commitBuilder.setParentId(parentCommitId);
            }

            ObjectId commitId = inserter.insert(commitBuilder);
            inserter.flush();
            return commitId;
        }
    }

    /**
     * Builds the commit tree as an OVERLAY on the parent, not a replacement of it.
     *
     * <p>This used to start from an empty {@code DirCache.newInCore()} and add only the files being
     * committed, so a single-file upload produced a tree containing exactly that file. The parent was
     * still attached via {@code setParentId}, which made the commit graph look normal while every
     * other file on the branch vanished from HEAD. app-web calls this path from its upload button.
     *
     * <p>{@link DirCacheEditor} rather than {@link DirCacheBuilder#add}: builder.add APPENDS. Adding
     * an entry for a path the parent tree already carries would leave the DirCache holding two
     * entries for one path. PathEdit replaces-or-inserts, which is the overlay semantics wanted here.
     */
    private ObjectId writeTree(Repository repository,
            ObjectInserter inserter,
            List<CommitFile> files,
            ObjectId parentCommitId) throws IOException {
        DirCache dirCache = DirCache.newInCore();

        DirCacheBuilder builder = dirCache.builder();
        if (parentCommitId != null) {
            try (RevWalk walk = new RevWalk(repository);
                    ObjectReader reader = repository.newObjectReader()) {
                RevCommit parent = walk.parseCommit(parentCommitId);
                builder.addTree(new byte[0], DirCacheEntry.STAGE_0, reader, parent.getTree());
            }
        }
        builder.finish();

        // Blobs are inserted before the editor runs. DirCacheEditor.PathEdit#apply cannot throw a
        // checked exception, and insertion is the part that fails, so it must not happen in there.
        List<StagedFile> staged = stage(inserter, files);
        if (!staged.isEmpty()) {
            DirCacheEditor editor = dirCache.editor();
            for (StagedFile file : staged) {
                editor.add(new DirCacheEditor.PathEdit(file.path()) {
                    @Override
                    public void apply(DirCacheEntry entry) {
                        entry.setFileMode(FileMode.REGULAR_FILE);
                        entry.setObjectId(file.blobId());
                    }
                });
            }
            editor.finish();
        }

        return dirCache.writeTree(inserter);
    }

    private List<StagedFile> stage(ObjectInserter inserter, List<CommitFile> files) {
        List<StagedFile> staged = new ArrayList<>();
        if (files == null) {
            return staged;
        }
        files.stream()
                .filter(file -> file != null && StringUtils.hasText(file.getPath()))
                .sorted(Comparator.comparing(CommitFile::getPath))
                .forEach(file -> staged.add(stageOne(inserter, file)));
        return staged;
    }

    private StagedFile stageOne(ObjectInserter inserter, CommitFile file) {
        try {
            byte[] content = file.getContent() != null ? file.getContent() : new byte[0];
            return new StagedFile(normalizePath(file.getPath()),
                    inserter.insert(Constants.OBJ_BLOB, content));
        } catch (IOException e) {
            throw new CommitFailedException(
                    "Failed to stage commit file: " + file.getPath(), e);
        }
    }

    private record StagedFile(String path, ObjectId blobId) {
    }

    /**
     * @param expectedOldObjectId the head this commit's tree was built from, or {@code null} when the
     *     branch did not exist. Supplied by the caller rather than re-read here: re-reading compared
     *     the head against itself, so a commit that landed between tree construction and ref update
     *     satisfied the check and was then discarded. {@code zeroId} means "must not exist yet",
     *     which is the same guarantee for branch creation.
     */
    // Package-private so a test can hand it a deliberately stale parent. The race it guards
    // (a commit landing between tree construction and ref update) cannot be triggered
    // deterministically through commit(), and a threaded test that only sometimes races would
    // pass whether or not the CAS works.
    void updateBranchReference(Repository repository, String branch, ObjectId commitId,
            ObjectId expectedOldObjectId) throws IOException {
        String branchRef = Constants.R_HEADS + branch;

        RefUpdate refUpdate = repository.updateRef(branchRef);
        refUpdate.setExpectedOldObjectId(
                expectedOldObjectId != null ? expectedOldObjectId : ObjectId.zeroId());
        refUpdate.setNewObjectId(commitId);
        refUpdate.setRefLogMessage("commit: " + branch, false);

        RefUpdate.Result result = refUpdate.update();
        switch (result) {
            case NEW:
            case FAST_FORWARD:
            case FORCED:
            case NO_CHANGE:
                return;
            default:
                throw new IOException("Failed to update branch ref: " + result);
        }
    }

    private ObjectId resolveBranchHead(Repository repository, String branch) throws IOException {
        Ref ref = repository.findRef(Constants.R_HEADS + branch);
        return ref == null ? null : ref.getObjectId();
    }

    private PersonIdent createPersonIdent(String authorName, String authorEmail) {
        String resolvedName = StringUtils.hasText(authorName) ? authorName.trim() : "jgitkins";
        String resolvedEmail = StringUtils.hasText(authorEmail) ? authorEmail.trim() : "noreply@jgitkins.local";
        return new PersonIdent(resolvedName, resolvedEmail, Instant.now(), ZoneId.systemDefault());
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new CommitFailedException("Commit file path is required");
        }
        return path.trim().replace('\\', '/').replaceAll("^/+", "");
    }

    private CommitHistory toHistory(RevCommit revCommit) {
        PersonIdent author = revCommit.getAuthorIdent();
        return CommitHistory.builder()
                .hash(revCommit.getName())
                .shortHash(revCommit.abbreviate(7).name())
                .message(revCommit.getFullMessage())
                .authorName(author.getName())
                .authorEmail(author.getEmailAddress())
                .committedAt(author.getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
