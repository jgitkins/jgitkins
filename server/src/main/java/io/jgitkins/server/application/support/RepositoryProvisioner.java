package io.jgitkins.server.application.support;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.application.factory.CommitFileFactory;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.application.port.out.CommitGitPort;
import io.jgitkins.server.application.port.out.RepositoryGitPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryPersistencePort repositoryPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    public Repository provision(Repository repository, InitialCommitOptions initialCommitOptions) {
        initializeGitRepository(repository);
        createDefaultBranch(repository);
        return initializeContentIfNeeded(repository, initialCommitOptions);
    }

    private void initializeGitRepository(Repository repository) {
        repositoryGitPort.initialize(
                repositoryNamespaceResolver.resolve(repository),
                repository.getName().getValue()
        );
    }

    private void createDefaultBranch(Repository repository) {
        Branch defaultBranch = Branch.create(
                repository.getId().getValue(),
                repository.getDefaultBranch().getValue(),
                false,
                true,
                true
        );
        branchPort.save(defaultBranch);
    }

    private Repository initializeContentIfNeeded(Repository repository, InitialCommitOptions initialCommitOptions) {
        if (initialCommitOptions == null || !initialCommitOptions.requiresInitialContent()) {
            return repository;
        }

        String namespace = repositoryNamespaceResolver.resolve(repository);
        String repoName = repository.getName().getValue();
        String branchName = repository.getDefaultBranch().getValue();

        List<CommitFile> files = commitFileFactory.prepareInitialFile(repoName);
        commitGitPort.commit(
                namespace,
                repoName,
                branchName,
                initialCommitOptions.commitMessage(),
                initialCommitOptions.authorName(),
                initialCommitOptions.authorEmail(),
                files
        );
        repositoryGitPort.updateHeadReference(namespace, repoName, branchName);

        return repositoryPort.update(repository.markInit(LocalDateTime.now()));
    }
}
