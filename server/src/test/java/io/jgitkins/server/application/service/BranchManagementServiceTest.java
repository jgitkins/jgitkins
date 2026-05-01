package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.branch.BranchWritePolicy;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.repository.BranchRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchManagementServiceTest {

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;
    @Mock
    private BranchGitPort branchGitPort;
    @Mock
    private BranchRepository branchPort;
    @Mock
    private RepositoryPersistencePort repositoryPort;

    private BranchManagementService service;

    @BeforeEach
    void setUp() {
        BranchCreationValidator branchCreationValidator = new BranchCreationValidator(branchPort);
        BranchWritePolicy branchWritePolicy = new BranchWritePolicy(branchCreationValidator);
        service = new BranchManagementService(
                repositoryNamespaceResolver,
                branchWritePolicy,
                repositoryAccessValidator,
                branchGitPort,
                branchPort,
                repositoryPort
        );
    }

    @Test
    void createBranch_createsBranchInGitAndPersistence() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.isInitialized()).thenReturn(true);
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.empty());
        when(branchPort.findByRepositoryIdAndName(1L, "main"))
                .thenReturn(Optional.of(Branch.create(1L, "main", false, true, true)));

        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", "main", false);

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
        InOrder inOrder = inOrder(branchPort, branchGitPort);
        inOrder.verify(branchPort).delete(branch);
        inOrder.verify(branchGitPort).deleteBranch("org", "repo", "feature");
    }

    @Test
    void deleteBranch_throwsWhenBranchMissing() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "missing")).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.deleteBranch(1L, "missing"));
    }

    @Test
    void deleteBranch_throwsWhenBranchIsDefault() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch defaultBranch = Branch.create(1L, "main", false, false, true);

        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "main")).thenReturn(Optional.of(defaultBranch));

        assertThrows(JgitkinsException.class, () -> service.deleteBranch(1L, "main"));
    }
}
