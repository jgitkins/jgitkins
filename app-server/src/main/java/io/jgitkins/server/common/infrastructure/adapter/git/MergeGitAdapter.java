package io.jgitkins.server.common.infrastructure.adapter.git;

import io.jgitkins.server.shared.common.GitConstants;
import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.port.out.MergeGitPort;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MergeGitAdapter implements MergeGitPort {

    private final String rootPath;

    public MergeGitAdapter(@Value("${jgitkins.server.runtime.volume:${user.home}}")
                           String runtimeVolume) {
        this.rootPath = runtimeVolume;
    }

    @Override
    public MergeResult previewMergeability(String namespace, String repoName, String sourceBranch, String targetBranch)
            throws IOException {
        try (Repository repo = openRepository(namespace, repoName)) {
            MergeRefs refs = resolveMergeRefs(repo, sourceBranch, targetBranch);
            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit sourceCommit = rw.parseCommit(refs.sourceId());
                RevCommit targetCommit = rw.parseCommit(refs.targetId());

                if (isAlreadyUpToDate(rw, sourceCommit, targetCommit)) {
                    return alreadyUpToDatePreview(sourceBranch, targetBranch, targetCommit.getTree().getId());
                }

                boolean fastForwardPossible = isMergedInto(rw, targetCommit, sourceCommit);
                if (!hasCommonAncestor(rw, sourceCommit, targetCommit)) {
                    return noCommonAncestorPreview(sourceBranch, targetBranch);
                }

                ResolveMerger merger = (ResolveMerger) MergeStrategy.RECURSIVE.newMerger(repo, true);
                if (!merger.merge(targetCommit, sourceCommit)) {
                    return conflictPreview(sourceBranch, targetBranch, merger, fastForwardPossible);
                }

                ObjectId mergedTreeId = merger.getResultTreeId();
                if (isTargetTree(mergedTreeId, targetCommit)) {
                    return alreadyUpToDatePreview(sourceBranch, targetBranch, mergedTreeId);
                }

                return mergeablePreview(sourceBranch, targetBranch, mergedTreeId, fastForwardPossible);
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
            if (sourceId == null) {
                throw new IllegalArgumentException("Source branch not found: " + req.getSourceBranch());
            }
            if (targetId == null) {
                throw new IllegalArgumentException("Target branch not found: " + req.getTargetBranch());
            }

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

                PersonIdent author = new PersonIdent(req.getAuthorName(), req.getAuthorEmail(), Instant.now(),
                        ZoneId.systemDefault());
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

    private Repository openRepository(String namespace, String repoName) throws IOException {
        File bareRepositoryPath = new File(rootPath + "/" + namespace + "/" + repoName + ".git");
        return new FileRepositoryBuilder()
                .setGitDir(bareRepositoryPath)
                .setMustExist(true)
                .build();
    }

    private MergeRefs resolveMergeRefs(Repository repo, String sourceBranch, String targetBranch) throws IOException {
        ObjectId sourceId = resolveRef(repo, GitConstants.REFS_HEADS_PREFIX + sourceBranch);
        ObjectId targetId = resolveRef(repo, GitConstants.REFS_HEADS_PREFIX + targetBranch);
        if (sourceId == null) {
            throw new IllegalArgumentException("Source branch not found: " + sourceBranch);
        }
        if (targetId == null) {
            throw new IllegalArgumentException("Target branch not found: " + targetBranch);
        }
        return new MergeRefs(sourceId, targetId);
    }

    private boolean isAlreadyUpToDate(RevWalk rw, RevCommit sourceCommit, RevCommit targetCommit) throws IOException {
        return Objects.equals(sourceCommit.getTree().getId(), targetCommit.getTree().getId())
                || isMergedInto(rw, sourceCommit, targetCommit);
    }

    private boolean isMergedInto(RevWalk rw, RevCommit baseCommit, RevCommit tipCommit) throws IOException {
        rw.reset();
        return rw.isMergedInto(baseCommit, tipCommit);
    }

    private boolean hasCommonAncestor(RevWalk rw, RevCommit sourceCommit, RevCommit targetCommit) throws IOException {
        rw.reset();
        rw.markStart(sourceCommit);
        rw.markStart(targetCommit);
        rw.setRevFilter(RevFilter.MERGE_BASE);
        RevCommit base = rw.next();
        rw.reset();
        return base != null;
    }

    private boolean isTargetTree(ObjectId mergedTreeId, RevCommit targetCommit) {
        return mergedTreeId != null && mergedTreeId.equals(targetCommit.getTree().getId());
    }

    private MergeResult alreadyUpToDatePreview(String sourceBranch, String targetBranch, ObjectId resultTreeId) {
        MergeResult result = previewResult(MergeResult.Status.ALREADY_UP_TO_DATE, sourceBranch, targetBranch);
        result.setResultTreeId(resultTreeId.name());
        return result;
    }

    private MergeResult noCommonAncestorPreview(String sourceBranch, String targetBranch) {
        return previewResult(MergeResult.Status.CONFLICTS, sourceBranch, targetBranch);
    }

    private MergeResult conflictPreview(String sourceBranch,
                                        String targetBranch,
                                        ResolveMerger merger,
                                        boolean fastForwardPossible) {
        MergeResult result = previewResult(MergeResult.Status.CONFLICTS, sourceBranch, targetBranch);
        result.setFastForwardPossible(fastForwardPossible);
        result.setMergeCommitRequired(!fastForwardPossible);
        if (merger.getUnmergedPaths() != null) {
            result.setConflicts(new ArrayList<>(merger.getUnmergedPaths()));
        }
        return result;
    }

    private MergeResult mergeablePreview(String sourceBranch,
                                         String targetBranch,
                                         ObjectId mergedTreeId,
                                         boolean fastForwardPossible) {
        MergeResult result = previewResult(MergeResult.Status.MERGEABLE, sourceBranch, targetBranch);
        result.setFastForwardPossible(fastForwardPossible);
        result.setMergeCommitRequired(!fastForwardPossible);
        result.setResultTreeId(mergedTreeId.name());
        return result;
    }

    private MergeResult previewResult(MergeResult.Status status, String sourceBranch, String targetBranch) {
        MergeResult result = new MergeResult();
        result.setStatus(status);
        result.setSourceBranch(sourceBranch);
        result.setTargetBranch(targetBranch);
        return result;
    }

    private record MergeRefs(ObjectId sourceId, ObjectId targetId) {}
}
