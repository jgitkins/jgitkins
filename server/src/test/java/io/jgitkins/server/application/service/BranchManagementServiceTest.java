package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.exception.BranchAlreadyExistsException;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.exception.SourceBranchNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefAlreadyExistsException;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import io.jgitkins.server.repository.application.port.out.exception.GitSourceBranchRefMissingException;
import io.jgitkins.server.repository.application.support.branch.BranchFactory;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.BranchCreationValidator;
import io.jgitkins.server.application.validate.RepositoryAccessValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
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
    private RepositoryRepository repositoryRepository;

    private BranchManagementService service;

    @BeforeEach
    void setUp() {
        BranchCreationValidator branchCreationValidator = new BranchCreationValidator(branchPort);
        BranchFactory branchFactory = new BranchFactory(branchCreationValidator, branchPort, branchGitPort);
        service = new BranchManagementService(
                repositoryNamespaceResolver,
                repositoryAccessValidator,
                repositoryRepository,
                branchFactory,
                branchGitPort,
                branchPort
        );
    }

    @Test
    void createBranch_createsBranchInGitAndPersistence() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
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
    void createBranch_translatesGitSourceBranchRefMissingToApplicationException() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.isInitialized()).thenReturn(true);
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.empty());
        when(branchPort.findByRepositoryIdAndName(1L, "main"))
                .thenReturn(Optional.of(Branch.create(1L, "main", false, true, true)));
        doThrow(new GitSourceBranchRefMissingException("main"))
                .when(branchGitPort).createBranch(any());

        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", "main", false);

        assertThrows(SourceBranchNotFoundException.class, () -> service.createBranch(command));
    }

    @Test
    void createBranch_translatesGitBranchRefAlreadyExistsToApplicationException() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(repository.getId()).thenReturn(RepositoryId.of(1L));
        when(repository.isInitialized()).thenReturn(true);
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.empty());
        when(branchPort.findByRepositoryIdAndName(1L, "main"))
                .thenReturn(Optional.of(Branch.create(1L, "main", false, true, true)));
        doThrow(new GitBranchRefAlreadyExistsException("feature"))
                .when(branchGitPort).createBranch(any());

        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", "main", false);

        assertThrows(BranchAlreadyExistsException.class, () -> service.createBranch(command));
    }

    @Test
    void deleteBranch_deletesInGitAndPersistenceWhenNotDefaultBranch() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch branch = Branch.create(1L, "feature");

        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.of(branch));

        service.deleteBranch(1L, "feature");

        verify(repositoryAccessValidator).validateCanCommit("org", "repo");
        InOrder inOrder = inOrder(branchPort, branchGitPort);
        inOrder.verify(branchPort).delete(branch);
        inOrder.verify(branchGitPort).deleteBranch("org", "repo", "feature");
    }

    @Test
    void deleteBranch_translatesGitBranchRefMissingToApplicationException() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch branch = Branch.create(1L, "feature");

        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "feature")).thenReturn(Optional.of(branch));
        doThrow(new GitBranchRefMissingException("feature"))
                .when(branchGitPort).deleteBranch("org", "repo", "feature");

        assertThrows(BranchNotFoundException.class, () -> service.deleteBranch(1L, "feature"));
    }

    @Test
    void deleteBranch_throwsWhenBranchMissing() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "missing")).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.deleteBranch(1L, "missing"));
    }

    @Test
    void deleteBranch_throwsWhenBranchIsDefault() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        Branch defaultBranch = Branch.create(1L, "main", false, false, true);

        when(repository.getName()).thenReturn(RepositoryName.from("repo"));
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("org");
        when(branchPort.findByRepositoryIdAndName(1L, "main")).thenReturn(Optional.of(defaultBranch));

        assertThrows(JgitkinsException.class, () -> service.deleteBranch(1L, "main"));
    }
}
