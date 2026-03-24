package io.jgitkins.server.infrastructure.adapter.git;

import io.jgitkins.server.application.common.GitConstants;
import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.out.MergeGitPort;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class MergeGitAdapter implements MergeGitPort {

    private final String rootPath;

    public MergeGitAdapter(@Value("${jgitkins.server.runtime.volume:${user.home}}")
                           String runtimeVolume) {
        this.rootPath = runtimeVolume;
    }

    @Override
    public MergeResult previewMerge(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException {
        File bareRepositoryPath = new File(rootPath + "/" + namespace + "/" + repoName + ".git");

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(bareRepositoryPath)
                .setMustExist(true)
                .build()) {

            ObjectId sourceId = resolveRef(repo, GitConstants.REFS_HEADS_PREFIX + sourceBranch);
            ObjectId targetId = resolveRef(repo, GitConstants.REFS_HEADS_PREFIX + targetBranch);
            if (sourceId == null) throw new IllegalArgumentException("Source branch not found: " + sourceBranch);
            if (targetId == null) throw new IllegalArgumentException("Target branch not found: " + targetBranch);

            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit sourceCommit = rw.parseCommit(sourceId);
                RevCommit targetCommit = rw.parseCommit(targetId);

                if (Objects.equals(sourceCommit.getTree().getId(), targetCommit.getTree().getId())
                        || rw.isMergedInto(sourceCommit, targetCommit)) {
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.ALREADY_UP_TO_DATE);
                    r.setTargetBranch(targetBranch);
                    r.setSourceBranch(sourceBranch);
                    r.setResultTreeId(targetCommit.getTree().getId().name());
                    return r;
                }

                rw.reset();
                rw.markStart(sourceCommit);
                rw.markStart(targetCommit);
                rw.setRevFilter(RevFilter.MERGE_BASE);
                RevCommit base = rw.next();
                rw.reset();

                if (base == null) {
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.NO_COMMON_ANCESTOR);
                    r.setTargetBranch(targetBranch);
                    r.setSourceBranch(sourceBranch);
                    return r;
                }

                ResolveMerger merger = (ResolveMerger) MergeStrategy.RECURSIVE.newMerger(repo, true);
                boolean ok = merger.merge(targetCommit, sourceCommit);

                if (!ok) {
                    List<String> conflicts = new ArrayList<>();
                    if (merger.getUnmergedPaths() != null) {
                        conflicts.addAll(merger.getUnmergedPaths());
                    }
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.CONFLICTS);
                    r.setTargetBranch(targetBranch);
                    r.setSourceBranch(sourceBranch);
                    r.setConflicts(conflicts.isEmpty() ? null : conflicts);
                    return r;
                }

                ObjectId mergedTreeId = merger.getResultTreeId();
                if (mergedTreeId != null && mergedTreeId.equals(targetCommit.getTree().getId())) {
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.ALREADY_UP_TO_DATE);
                    r.setTargetBranch(targetBranch);
                    r.setSourceBranch(sourceBranch);
                    r.setResultTreeId(mergedTreeId.name());
                    return r;
                }

                MergeResult r = new MergeResult();
                r.setStatus(MergeResult.Status.MERGEABLE);
                r.setTargetBranch(targetBranch);
                r.setSourceBranch(sourceBranch);
                r.setResultTreeId(mergedTreeId.name());
                return r;
            }
        }
    }

    @Override
    public MergeResult merge(String namespace, String repoName, MergeRequest req) throws IOException {
        File bareRepositoryPath = new File(rootPath + "/" + namespace + "/" + repoName + ".git");

        try (Repository repo = new FileRepositoryBuilder()
                .setGitDir(bareRepositoryPath)
                .setMustExist(true)
                .build()) {

            String targetRef = GitConstants.REFS_HEADS_PREFIX + req.getTargetBranch();
            String sourceRef = GitConstants.REFS_HEADS_PREFIX + req.getSourceBranch();

            ObjectId targetId = resolveRef(repo, targetRef);
            ObjectId sourceId = resolveRef(repo, sourceRef);
            if (sourceId == null)
                throw new IllegalArgumentException("Source branch not found: " + req.getSourceBranch());
            if (targetId == null)
                throw new IllegalArgumentException("Target branch not found: " + req.getTargetBranch());

            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit targetCommit = rw.parseCommit(targetId);
                RevCommit sourceCommit = rw.parseCommit(sourceId);

                if (Objects.equals(targetCommit.getTree().getId(), sourceCommit.getTree().getId())) {
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.ALREADY_UP_TO_DATE);
                    r.setTargetBranch(req.getTargetBranch());
                    r.setSourceBranch(req.getSourceBranch());
                    r.setResultTreeId(targetCommit.getTree().getId().name());
                    return r;
                }

                ResolveMerger merger = (ResolveMerger) MergeStrategy.RECURSIVE.newMerger(repo, true);
                boolean ok = merger.merge(targetCommit, sourceCommit);
                if (!ok) {
                    List<String> conflicts = merger.getUnmergedPaths() == null
                            ? List.of()
                            : new ArrayList<>(merger.getUnmergedPaths());

                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.CONFLICTS);
                    r.setTargetBranch(req.getTargetBranch());
                    r.setSourceBranch(req.getSourceBranch());
                    r.setConflicts(conflicts);
                    return r;
                }

                ObjectId mergedTreeId = merger.getResultTreeId();
                if (Objects.equals(mergedTreeId, targetCommit.getTree().getId())) {
                    MergeResult r = new MergeResult();
                    r.setStatus(MergeResult.Status.ALREADY_UP_TO_DATE);
                    r.setTargetBranch(req.getTargetBranch());
                    r.setSourceBranch(req.getSourceBranch());
                    r.setResultTreeId(mergedTreeId.name());
                    return r;
                }

                PersonIdent author = new PersonIdent(req.getAuthorName(), req.getAuthorEmail(), Instant.now(), ZoneId.systemDefault());
                PersonIdent committer = author;

                String msg = (req.getCommitMessage() == null || req.getCommitMessage().isBlank())
                        ? String.format("squash: merge '%s' into '%s'", req.getSourceBranch(), req.getTargetBranch())
                        : req.getCommitMessage();

                CommitBuilder cb = new CommitBuilder();
                cb.setTreeId(mergedTreeId);
                cb.setAuthor(author);
                cb.setCommitter(committer);
                cb.setMessage(msg);
                cb.setParentId(targetCommit);

                ObjectId newCommitId;
                try (ObjectInserter inserter = repo.newObjectInserter()) {
                    newCommitId = inserter.insert(cb);
                    inserter.flush();
                }

                RefUpdate ru = repo.updateRef(targetRef);
                ru.setExpectedOldObjectId(targetCommit.getId());
                ru.setNewObjectId(newCommitId);
                ru.setRefLogMessage("squash-merge " + req.getSourceBranch() + " -> " + req.getTargetBranch(), false);
                RefUpdate.Result res = ru.update();

                switch (res) {
                    case FAST_FORWARD:
                    case NEW:
                    case FORCED:
                    case NO_CHANGE:
                        break;
                    default:
                        throw new IllegalStateException("Ref update failed: " + res);
                }

                MergeResult r = new MergeResult();
                r.setStatus(MergeResult.Status.MERGED);
                r.setTargetBranch(req.getTargetBranch());
                r.setSourceBranch(req.getSourceBranch());
                r.setNewCommitId(newCommitId.name());
                r.setResultTreeId(mergedTreeId.name());
                return r;
            }
        }
    }

    private ObjectId resolveRef(Repository repo, String fullRef) throws IOException {
        Ref ref = repo.findRef(fullRef);
        return ref == null ? null : ref.getObjectId();
    }
}
