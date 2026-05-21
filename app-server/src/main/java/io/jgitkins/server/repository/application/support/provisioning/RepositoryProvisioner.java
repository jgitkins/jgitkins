package io.jgitkins.server.repository.application.support.provisioning;

import io.jgitkins.server.repository.application.contract.result.CommitFile;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryGitPort;
import io.jgitkins.server.common.factory.CommitFileFactory;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryProvisioner {

    private final CommitFileFactory commitFileFactory;
    private final RepositoryRepository repositoryRepository;
    private final BranchRepository branchPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    public Repository provision(Repository repository, InitialCommitOptions initialCommitOptions) {
        initializeGitRepository(repository);
        createDefaultBranch(repository);
        return initializeContentIfNeeded(repository, initialCommitOptions);
    }

    public void delete(Repository repository) {
        repositoryGitPort.deleteRepository(
                repositoryNamespaceResolver.resolve(repository),
                repository.getName().getValue()
        );
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

        return repositoryRepository.update(repository.markInit(LocalDateTime.now()));
    }
}
