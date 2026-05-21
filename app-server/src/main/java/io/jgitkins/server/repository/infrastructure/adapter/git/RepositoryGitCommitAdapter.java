package io.jgitkins.server.repository.infrastructure.adapter.git;

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
            ObjectId commitId = createCommit(repository, branch, message, authorName, authorEmail, files);
            updateBranchReference(repository, branch, commitId);
        } catch (IOException e) {
            throw new CommitFailedException("Failed to commit changes", e);
        }
    }

    private ObjectId createCommit(Repository repository,
            String branch,
            String message,
            String authorName,
            String authorEmail,
            List<CommitFile> files) throws IOException {
        String commitMessage = StringUtils.hasText(message) ? message.trim() : "Initial commit";
        PersonIdent ident = createPersonIdent(authorName, authorEmail);
        ObjectId parentCommitId = resolveBranchHead(repository, branch);

        try (ObjectInserter inserter = repository.newObjectInserter()) {
            ObjectId treeId = writeTree(inserter, files);

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

    private ObjectId writeTree(ObjectInserter inserter, List<CommitFile> files) throws IOException {
        DirCache dirCache = DirCache.newInCore();
        DirCacheBuilder builder = dirCache.builder();

        if (files != null) {
            files.stream()
                    .filter(file -> file != null && StringUtils.hasText(file.getPath()))
                    .sorted(Comparator.comparing(CommitFile::getPath))
                    .forEach(file -> builder.add(createEntry(inserter, file)));
        }

        builder.finish();
        return dirCache.writeTree(inserter);
    }

    private DirCacheEntry createEntry(ObjectInserter inserter, CommitFile file) {
        try {
            byte[] content = file.getContent() != null ? file.getContent() : new byte[0];
            ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, content);
            DirCacheEntry entry = new DirCacheEntry(normalizePath(file.getPath()));
            entry.setFileMode(FileMode.REGULAR_FILE);
            entry.setObjectId(blobId);
            return entry;
        } catch (IOException e) {
            throw new CommitFailedException(
                    "Failed to stage commit file: " + file.getPath(), e);
        }
    }

    private void updateBranchReference(Repository repository, String branch, ObjectId commitId) throws IOException {
        String branchRef = Constants.R_HEADS + branch;
        ObjectId currentHead = resolveBranchHead(repository, branch);

        RefUpdate refUpdate = repository.updateRef(branchRef);
        if (currentHead != null) {
            refUpdate.setExpectedOldObjectId(currentHead);
        }
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
