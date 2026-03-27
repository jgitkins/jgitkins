package io.jgitkins.runner.infrastructure.git;

import static io.jgitkins.runner.infrastructure.Constants.WORK_SPACE_ROOT;

import io.jgitkins.runner.application.port.out.RepositorySyncPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.FetchResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitRepositorySyncAdapter implements RepositorySyncPort {

    @Override
    public Path fetchRepository(String repoUrl, String taskCd, String repoName, String ref) throws Exception {
        Path workspace = Paths.get(WORK_SPACE_ROOT, taskCd, repoName);
        Files.createDirectories(workspace);

        if (!Files.exists(workspace.resolve(".git"))) {
            log.info("Cloning repository {} into {}", repoUrl, workspace);
            try (Git git = Git.cloneRepository()
                              .setURI(repoUrl)
                              .setDirectory(workspace.toFile())
                              .call()) {
                log.info("Clone completed: {}", git.getRepository().getDirectory());
            }
        } else {
            log.info("Repository already exists, fetching latest changes for {}", workspace);
            try (Git git = Git.open(workspace.toFile())) {
                FetchResult result = git.fetch()
                                        .setRemote("origin")
                                        .call();
                log.debug("Fetch result: {}", result.getMessages());
            }
        }

        try (Git git = Git.open(workspace.toFile())) {
            git.checkout().setName(ref).call();
        }

        return workspace;
    }
}
