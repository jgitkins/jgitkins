package io.jgitkins.server.application.support;

import io.jgitkins.server.shared.common.RepositoryPathHelper;
import io.jgitkins.server.application.dto.command.PushEventCommand;
import io.jgitkins.server.application.dto.command.PushHookRequest;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.domain.aggregate.Repository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PushEventCommandResolver {

    private final RepositoryQueryPort repositoryQueryPort;
    private final Path repoRootPath;

    public PushEventCommandResolver(
            RepositoryQueryPort repositoryQueryPort,
            @Value("${jgitkins.server.runtime.volume:${user.home}}") String runtimeVolume) {
        this.repositoryQueryPort = repositoryQueryPort;
        this.repoRootPath = Paths.get(runtimeVolume).toAbsolutePath().normalize();
    }

    public PushEventCommand resolve(PushHookRequest request) {
        Repository repository = resolveRepository(request.gitDirPath())
                .orElseThrow(() -> new RepositoryNotFoundException(
                        "Repository not found for path: " + request.gitDirPath()));

        String namespace = extractNamespace(repository)
                .orElseThrow(() -> new RepositoryNotFoundException(
                        "Repository namespace not found for path: " + request.gitDirPath()));

        return PushEventCommand.builder()
                .repositoryId(repository.getId().getValue())
                .namespace(namespace)
                .repoName(repository.getName().getValue())
                .branchName(request.branchName())
                .branchCreated(request.branchCreated())
                .branchDeleted(request.branchDeleted())
                .commitHash(request.commitHash())
                .triggeredBy(request.triggeredBy())
                .build();
    }

    private Optional<Repository> resolveRepository(String gitDirPath) {
        Optional<Repository> byStoredPath = repositoryQueryPort.findByPath(gitDirPath);
        if (byStoredPath.isPresent()) {
            return byStoredPath;
        }

        return toClonePath(gitDirPath)
                .flatMap(repositoryQueryPort::findByClonePath);
    }

    private Optional<String> extractNamespace(Repository repository) {
        if (repository == null || !StringUtils.hasText(repository.getClonePath())) {
            return Optional.empty();
        }

        String clonePath = repository.getClonePath().trim().replaceAll("^/+", "").replaceAll("/+$", "");
        int lastSlash = clonePath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return Optional.empty();
        }
        return Optional.of(clonePath.substring(0, lastSlash));
    }

    private Optional<String> toClonePath(String gitDirPath) {
        if (!StringUtils.hasText(gitDirPath)) {
            return Optional.empty();
        }

        Path absoluteGitDir = Paths.get(gitDirPath).toAbsolutePath().normalize();
        if (!absoluteGitDir.startsWith(repoRootPath)) {
            return Optional.empty();
        }

        Path relativePath = repoRootPath.relativize(absoluteGitDir);
        if (relativePath.getNameCount() < 2) {
            return Optional.empty();
        }

        String clonePath = RepositoryPathHelper.buildClonePath(
                relativePath.subpath(0, relativePath.getNameCount() - 1).toString().replace('\\', '/'),
                relativePath.getFileName().toString());
        return Optional.of(clonePath);
    }
}
