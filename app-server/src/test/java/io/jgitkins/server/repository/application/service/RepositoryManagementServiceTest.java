package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.translator.RepositoryApplicationMapper;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.application.service.internal.ownership.RepositoryOwnershipPolicy;
import io.jgitkins.server.repository.application.service.internal.provisioning.RepositoryProvisioner;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.repository.application.validate.RepositoryValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
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
    private RepositoryRepository repositoryRepository;
    @Mock
    private RepositoryProvisioner repositoryProvisioner;
    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Mock
    private OrganizationMembershipPort organizationMembershipPort;
    @Mock
    private RepositoryActorPort repositoryActorPort;
    @Mock
    private io.jgitkins.server.repository.application.policy.RepositoryDeletionPolicy repositoryDeletionPolicy;

    private RepositoryManagementService service;
    private RepositoryValidator repositoryValidator;

    @BeforeEach
    void setUp() {
        repositoryValidator = new RepositoryValidator(repositoryRepository, organizationMembershipPort);
        RepositoryOwnershipPolicy repositoryOwnershipPolicy = new RepositoryOwnershipPolicy(
                repositoryValidator,
                repositoryNamespaceResolver,
                repositoryDeletionPolicy
        );
        service = new RepositoryManagementService(
                repositoryApplicationMapper,
                repositoryProvisioner,
                repositoryRepository,
                repositoryOwnershipPolicy
        );
    }

    @Test
    void create_throwsWhenUserOwnerHasOrganizeId() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .requesterUserId(7L)
                .repoName("sample-repo")
                .ownerType(OwnerType.USER)
                .organizeId(10L)
                .mainBranch("main")
                .build();

        assertThrows(JgitkinsException.class, () -> service.create(command));
        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    void create_throwsWhenOrganizationOwnerWithoutMembership() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .requesterUserId(7L)
                .repoName("sample-repo")
                .ownerType(OwnerType.ORGANIZATION)
                .organizeId(10L)
                .mainBranch("main")
                .build();

        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.create(command));
        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    void create_savesWhenUserOwnerAndInputIsValid() {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .requesterUserId(7L)
                .repoName("sample-repo")
                .ownerType(OwnerType.USER)
                .mainBranch("main")
                .visibility(RepositoryVisibility.PUBLIC)
                .description("desc")
                .build();
        Repository saved = org.mockito.Mockito.mock(Repository.class);
        RepositoryResult result = new RepositoryResult(100L, null, "sample-repo", null, null, null, null, null, null, null, null, false, null, null, null);

        when(repositoryRepository.findByOwnerAndName(OwnerType.USER, RepositoryOwnerId.of(7L), RepositoryName.from("sample-repo")))
                .thenReturn(Optional.empty());
        when(repositoryNamespaceResolver.resolve(OwnerType.USER, RepositoryOwnerId.of(7L))).thenReturn("alice");
        when(repositoryRepository.save(any(Repository.class))).thenReturn(saved);
        when(repositoryProvisioner.provision(any(Repository.class), any(InitialCommitOptions.class))).thenReturn(saved);
        when(repositoryApplicationMapper.toDto(saved)).thenReturn(result);

        RepositoryResult response = service.create(command);

        assertEquals(100L, response.id());
        verify(repositoryProvisioner).provision(any(Repository.class), any(InitialCommitOptions.class));
    }

    /**
     * Replaced for task 2.83. This test used to mock an ORGANIZATION-owned repository and assert that
     * requester 7 deleted it successfully, with no membership stubbed anywhere -- it pinned the missing
     * authorization as intended behaviour. The service delegates the decision, so the service test now
     * asserts the delegation and the outcome; who may delete is decided in RepositoryDeletionPolicyTest.
     */
    @Test
    void deleteRepository_deletesWhenThePolicyAllowsIt() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));

        service.deleteRepository(7L, 1L);

        verify(repositoryDeletionPolicy).validateCanDelete(7L, repository);
        verify(repositoryProvisioner).delete(repository);
        verify(repositoryRepository).deleteById(RepositoryId.of(1L));
    }

    @Test
    void deleteRepository_doesNotDeleteWhenThePolicyRefuses() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryRepository.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        org.mockito.Mockito.doThrow(new io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException("nope"))
                .when(repositoryDeletionPolicy).validateCanDelete(7L, repository);

        assertThrows(JgitkinsException.class, () -> service.deleteRepository(7L, 1L));

        // The git directory must not be removed for a refused delete: the provisioner is irreversible.
        verify(repositoryProvisioner, never()).delete(any(Repository.class));
        verify(repositoryRepository, never()).deleteById(any(RepositoryId.class));
    }
}
