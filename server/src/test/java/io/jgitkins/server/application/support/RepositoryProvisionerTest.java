package io.jgitkins.server.repository.application.support.provisioning;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryGitPort;
import io.jgitkins.server.common.factory.CommitFileFactory;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.domain.repository.RepositoryRepository;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryProvisionerTest {

    @Mock
    private CommitFileFactory commitFileFactory;
    @Mock
    private RepositoryRepository repositoryRepository;
    @Mock
    private BranchRepository branchPort;
    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Mock
    private CommitGitPort commitGitPort;
    @Mock
    private RepositoryGitPort repositoryGitPort;

    private RepositoryProvisioner repositoryProvisioner;

    @BeforeEach
    void setUp() {
        repositoryProvisioner = new RepositoryProvisioner(
                commitFileFactory,
                repositoryRepository,
                branchPort,
                repositoryNamespaceResolver,
                commitGitPort,
                repositoryGitPort
        );
    }

    @Test
    void provision_initializesBareRepositoryAndCreatesDefaultBranchWithoutInitialCommit() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.getName()).thenReturn(RepositoryName.from("sample-repo"));
        when(repository.getDefaultBranch()).thenReturn(BranchName.of("main"));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("alice");

        Repository provisioned = repositoryProvisioner.provision(
                repository,
                InitialCommitOptions.of(false, null, null, null)
        );

        assertSame(repository, provisioned);
        verify(repositoryGitPort).initialize("alice", "sample-repo");
        verify(branchPort).save(any(Branch.class));
        verify(commitGitPort, never()).commit(any(), any(), any(), any(), any(), any(), any());
        verify(repositoryGitPort, never()).updateHeadReference(any(), any(), any());
        verify(repositoryRepository, never()).update(any(Repository.class));
    }

    @Test
    void provision_updatesHeadAndRepositoryWhenInitialContentIsRequired() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Repository initialized = org.mockito.Mockito.mock(Repository.class);
        List<CommitFile> files = List.of(CommitFile.builder().path("README.md").content(new byte[0]).build());

        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.getName()).thenReturn(RepositoryName.from("sample-repo"));
        when(repository.getDefaultBranch()).thenReturn(BranchName.of("main"));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("alice");
        when(commitFileFactory.prepareInitialFile("sample-repo")).thenReturn(files);
        when(repository.markInit(any())).thenReturn(initialized);
        when(repositoryRepository.update(initialized)).thenReturn(initialized);

        Repository provisioned = repositoryProvisioner.provision(
                repository,
                InitialCommitOptions.of(true, "init repo", "author", "author@example.com")
        );

        assertSame(initialized, provisioned);
        verify(repositoryGitPort).initialize("alice", "sample-repo");
        verify(branchPort).save(any(Branch.class));
        verify(commitGitPort).commit(
                "alice",
                "sample-repo",
                "main",
                "init repo",
                "author",
                "author@example.com",
                files
        );
        verify(repositoryGitPort).updateHeadReference("alice", "sample-repo", "main");
        verify(repositoryRepository).update(initialized);
    }
}
