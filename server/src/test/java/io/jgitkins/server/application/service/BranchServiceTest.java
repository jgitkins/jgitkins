package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.mapper.BranchApplicationMapper;
import io.jgitkins.server.application.port.out.BranchGitPort;
import io.jgitkins.server.application.port.out.BranchPersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;

    @Mock
    private BranchCreationValidator branchCreationValidator;

    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;

    @Mock
    private BranchApplicationMapper branchApplicationMapper;

    @Mock
    private BranchGitPort branchGitPort;

    @Mock
    private BranchPersistencePort branchPort;

    @Mock
    private RepositoryPersistencePort repositoryPort;

    @InjectMocks
    private BranchService service;

    @Test
    void createBranch_createsBranchInGitAndPersistence() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");

        BranchCreateCommand command = BranchCreateCommand.builder()
                .repositoryId(1L)
                .branchName("feature")
                .sourceBranch("main")
                .build();

        service.createBranch(command);

        verify(repositoryAccessValidator).validateCanCommit("org", "repo");
        verify(branchGitPort).createBranch(any());
        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchPort).save(captor.capture());
        Branch created = captor.getValue();
        assertEquals(1L, created.getRepositoryId());
        assertEquals("feature", created.getName());
    }

    @Test
    void deleteBranch_deletesInGitAndPersistenceWhenNotDefaultBranch() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch branch = Branch.create(1L, "feature");

        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.of(branch));

        service.deleteBranch(1L, "feature");

        verify(repositoryAccessValidator).validateCanCommit("org", "repo");
        verify(branchCreationValidator).validateNotDefaultBranch(repository, branch);
        verify(branchGitPort).deleteBranch("org", "repo", "feature");
        verify(branchPort).deleteByRepositoryIdAndName(1L, "feature");
    }

    @Test
    void getBranch_throwsWhenBranchMissing() {
        when(branchPort.findByRepositoryIdAndName(1L, "missing")).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.getBranch(1L, "missing"));
    }
}
