package io.jgitkins.server.infrastructure.adapter.git;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.out.FileGitPort;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryGitFileAdapter implements FileGitPort {

    private static final String TYPE_TREE = "tree";
    private static final String TYPE_BLOB = "blob";

    private final RepositoryResolver repositoryResolver;

    @Override
    public List<FileEntry> listTree(String namespace, String repoName, String branch, String directory) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            RevTree commitTree = resolveCommitTree(repository, branch);
            return listFiles(repository, commitTree, directory);
        } catch (IOException e) {
            throw new InfrastructureException(InfrastructureErrorCode.FILE_LOAD_FAILED, "Failed to list files", e);
        }
    }

    @Override
    public List<FileEntry> listAllFiles(String namespace, String repoName, String branch) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            RevTree tree = resolveCommitTree(repository, branch);
            return collectAllFileEntries(repository, tree);
        } catch (IOException e) {
            throw new InfrastructureException(InfrastructureErrorCode.FILE_LOAD_FAILED, "Failed to list all files", e);
        }
    }

    @Override
    public boolean exists(String namespace, String repoName, String branch, String filePath) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            RevTree tree = resolveCommitTree(repository, branch);
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, tree)) {
                return treeWalk != null;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private RevTree resolveCommitTree(Repository repository, String branch) throws IOException {
        ObjectId headId = repository.resolve(branch);
        // TODO: do not known application exception from adapter
        if (headId == null) {
            throw new ApplicationException(ApplicationErrorCode.BRANCH_NOT_FOUND, "Branch not found: " + branch);
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(headId);
            return commit.getTree();
        }
    }

    private List<FileEntry> listFiles(Repository repository, RevTree commitTree, String directory) throws IOException {
        if (directory == null || directory.isBlank() || ".".equals(directory)) {
            return listTopLevelEntries(repository, commitTree);
        }
        return listDirectoryEntries(repository, commitTree, directory);
    }

    private List<FileEntry> listTopLevelEntries(Repository repository, RevTree commitTree) throws IOException {
        List<FileEntry> entries = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(commitTree);
            treeWalk.setRecursive(false);
            while (treeWalk.next()) {
                entries.add(buildEntry(repository, treeWalk.getObjectId(0), treeWalk.getPathString(),
                        treeWalk.getFileMode(0)));
            }
        }
        return entries;
    }

    private List<FileEntry> listDirectoryEntries(Repository repository, RevTree commitTree, String directory)
            throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, directory, commitTree)) {
            if (treeWalk == null || treeWalk.getFileMode(0) != FileMode.TREE) {
                return List.of();
            }
            ObjectId treeId = treeWalk.getObjectId(0);
            return listTree(repository, treeId, directory, false);
        }
    }

    private List<FileEntry> listTree(Repository repo, ObjectId treeId, String prefix, boolean recursive)
            throws IOException {
        List<FileEntry> entries = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(treeId);
            treeWalk.setRecursive(recursive);
            while (treeWalk.next()) {
                String fullPath = prefix.isEmpty() ? treeWalk.getPathString() : prefix + "/" + treeWalk.getPathString();
                entries.add(buildEntry(repo, treeWalk.getObjectId(0), fullPath, treeWalk.getFileMode(0)));
            }
        }
        return entries;
    }

    private FileEntry buildEntry(Repository repo, ObjectId id, String fullPath, FileMode mode) throws IOException {
        String name = fullPath.contains("/") ? fullPath.substring(fullPath.lastIndexOf("/") + 1) : fullPath;
        boolean isDirectory = mode == FileMode.TREE;
        long size = isDirectory ? 0 : repo.open(id).getSize();
        String type = isDirectory ? TYPE_TREE : TYPE_BLOB;

        return FileEntry.builder()
                .name(name)
                .path(fullPath)
                .type(type)
                .isDirectory(isDirectory)
                .size(size)
                .build();
    }

    private List<FileEntry> collectAllFileEntries(Repository repository, RevTree tree) throws IOException {
        List<FileEntry> entries = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                entries.add(buildEntry(repository, treeWalk.getObjectId(0), treeWalk.getPathString(),
                        treeWalk.getFileMode(0)));
            }
        }
        return entries;
    }
}
