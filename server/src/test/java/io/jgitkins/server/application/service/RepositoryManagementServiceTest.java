package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.repository.application.support.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.support.provisioning.RepositoryProvisioner;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.model.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryManagementServiceTest {

    @Mock
    private RepositoryApplicationMapper repositoryApplicationMapper;
    @Mock
    private RepositoryPersistencePort repositoryPort;
    @Mock
    private RepositoryProvisioner repositoryProvisioner;
    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;
    @Mock
    private CurrentUserPort currentUserPersistencePort;

    private RepositoryManagementService service;
    private RepositoryValidator repositoryValidator;

    @BeforeEach
    void setUp() {
        repositoryValidator = new RepositoryValidator(repositoryPort, organizeMemberPort, currentUserPersistencePort);
        RepositoryOwnershipPolicy repositoryOwnershipPolicy = new RepositoryOwnershipPolicy(
                repositoryValidator,
                repositoryNamespaceResolver
        );
        service = new RepositoryManagementService(
                repositoryApplicationMapper,
                repositoryProvisioner,
                repositoryPort,
                repositoryOwnershipPolicy
        );
    }

    @Test
    void create_throwsWhenUserOwnerHasOrganizeId() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .repoName("sample-repo")
                .ownerType(OwnerType.USER)
                .organizeId(10L)
                .mainBranch("main")
                .build();

        assertThrows(JgitkinsException.class, () -> service.create(command));
        verify(repositoryPort, never()).save(any(Repository.class));
    }

    @Test
    void create_throwsWhenOrganizationOwnerWithoutMembership() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .repoName("sample-repo")
                .ownerType(OwnerType.ORGANIZATION)
                .organizeId(10L)
                .mainBranch("main")
                .build();

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(organizeMemberPort.existsByOrganizeIdAndUserId(OrganizeId.of(10L), UserId.of(7L))).thenReturn(false);

        assertThrows(JgitkinsException.class, () -> service.create(command));
        verify(repositoryPort, never()).save(any(Repository.class));
    }

    @Test
    void create_savesWhenUserOwnerAndInputIsValid() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .repoName("sample-repo")
                .ownerType(OwnerType.USER)
                .mainBranch("main")
                .visibility(RepositoryVisibility.PUBLIC)
                .description("desc")
                .build();
        Repository saved = org.mockito.Mockito.mock(Repository.class);
        RepositoryResult result = new RepositoryResult(100L, null, "sample-repo", null, null, null, null, null, null, null, null, false, null, null, null);

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(repositoryPort.findByOwnerAndName(OwnerType.USER, OwnerId.of(7L), RepositoryName.from("sample-repo")))
                .thenReturn(Optional.empty());
        when(repositoryNamespaceResolver.resolve(OwnerType.USER, OwnerId.of(7L))).thenReturn("alice");
        when(repositoryPort.save(any(Repository.class))).thenReturn(saved);
        when(repositoryProvisioner.provision(any(Repository.class), any(InitialCommitOptions.class))).thenReturn(saved);
        when(repositoryApplicationMapper.toDto(saved)).thenReturn(result);

        RepositoryResult response = service.create(command);

        assertEquals(100L, response.id());
        verify(repositoryProvisioner).provision(any(Repository.class), any(InitialCommitOptions.class));
    }

    @Test
    void deleteRepository_throwsWhenDeletingOtherUsersRepository() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repository.getOwnerType()).thenReturn(OwnerType.USER);
        when(repository.getOwnerId()).thenReturn(OwnerId.of(10L));
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(20L));

        assertThrows(JgitkinsException.class, () -> service.deleteRepository(1L));

        verify(repositoryProvisioner, never()).delete(any(Repository.class));
        verify(repositoryPort, never()).deleteById(any(RepositoryId.class));
    }

    @Test
    void deleteRepository_deletesWhenAccessible() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repository.getOwnerType()).thenReturn(OwnerType.ORGANIZATION);

        service.deleteRepository(1L);

        verify(repositoryProvisioner).delete(repository);
        verify(repositoryPort).deleteById(RepositoryId.of(1L));
    }
}
