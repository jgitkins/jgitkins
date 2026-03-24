package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.RepositoryGitPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.application.support.RepositoryLookupService;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.model.vo.UserId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryLifecycleServiceTest {

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Mock
    private RepositoryApplicationMapper repositoryApplicationMapper;
    @Mock
    private io.jgitkins.server.application.common.event.DomainEventPublisher domainEventPublisher;
    @Mock
    private RepositoryGitPort repositoryGitPort;
    @Mock
    private RepositoryPersistencePort repositoryPort;
    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;
    @Mock
    private OrganizePersistencePort organizePort;
    @Mock
    private CurrentUserPort currentUserPersistencePort;
    @Mock
    private UserPersistencePort userPort;

    private RepositoryLifecycleService service;

    @BeforeEach
    void setUp() {
        RepositoryValidator validator = new RepositoryValidator(repositoryPort, organizeMemberPort, currentUserPersistencePort);
        RepositoryLookupService lookupService = new RepositoryLookupService(repositoryPort, userPort, organizePort, organizeMemberPort);
        service = new RepositoryLifecycleService(
                repositoryNamespaceResolver,
                repositoryApplicationMapper,
                domainEventPublisher,
                repositoryGitPort,
                repositoryPort,
                currentUserPersistencePort,
                userPort,
                validator,
                lookupService
        );
    }

    @Test
    void getRepository_returnsMappedResult() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        RepositoryResult result = RepositoryResult.builder().id(1L).build();
        when(repositoryApplicationMapper.toDto(repository)).thenReturn(result);

        RepositoryResult response = service.getRepository(1L);

        assertEquals(1L, response.getId());
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
        RepositoryResult result = RepositoryResult.builder().id(100L).name("sample-repo").build();

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(repositoryPort.findByOwnerAndName(OwnerType.USER, OwnerId.of(7L), RepositoryName.from("sample-repo")))
                .thenReturn(Optional.empty());
        when(repositoryNamespaceResolver.resolve(OwnerType.USER, OwnerId.of(7L))).thenReturn("alice");
        when(repositoryPort.save(any(Repository.class))).thenReturn(saved);
        when(saved.getDomainEvents()).thenReturn(List.of());
        when(repositoryApplicationMapper.toDto(saved)).thenReturn(result);

        RepositoryResult response = service.create(command);

        assertEquals(100L, response.getId());
        verify(repositoryGitPort).initialize("alice", "sample-repo");
        verify(domainEventPublisher).publish(anyList());
    }

    @Test
    void getRepositories_returnsOnlyVisibleRepositoriesForRequester() {
        Repository publicRepo = org.mockito.Mockito.mock(Repository.class);
        Repository myPrivateRepo = org.mockito.Mockito.mock(Repository.class);
        Repository orgPrivateRepo = org.mockito.Mockito.mock(Repository.class);
        Repository notVisibleRepo = org.mockito.Mockito.mock(Repository.class);

        when(publicRepo.getVisibility()).thenReturn(RepositoryVisibility.PUBLIC);

        when(myPrivateRepo.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(myPrivateRepo.getOwnerType()).thenReturn(OwnerType.USER);
        when(myPrivateRepo.getOwnerId()).thenReturn(OwnerId.of(7L));

        when(orgPrivateRepo.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(orgPrivateRepo.getOwnerType()).thenReturn(OwnerType.ORGANIZATION);
        when(orgPrivateRepo.getOwnerId()).thenReturn(OwnerId.of(10L));

        when(notVisibleRepo.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(notVisibleRepo.getOwnerType()).thenReturn(OwnerType.USER);
        when(notVisibleRepo.getOwnerId()).thenReturn(OwnerId.of(99L));

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(repositoryPort.findAll()).thenReturn(List.of(publicRepo, myPrivateRepo, orgPrivateRepo, notVisibleRepo));
        when(organizeMemberPort.existsByOrganizeIdAndUserId(OrganizeId.of(10L), UserId.of(7L))).thenReturn(true);

        when(repositoryApplicationMapper.toDto(publicRepo)).thenReturn(RepositoryResult.builder().id(1L).name("public").build());
        when(repositoryApplicationMapper.toDto(myPrivateRepo)).thenReturn(RepositoryResult.builder().id(2L).name("mine").build());
        when(repositoryApplicationMapper.toDto(orgPrivateRepo)).thenReturn(RepositoryResult.builder().id(3L).name("org").build());

        List<RepositoryResult> response = service.getRepositories();

        assertEquals(3, response.size());
        assertEquals(List.of("public", "mine", "org"), response.stream().map(RepositoryResult::getName).toList());
    }

    @Test
    void deleteRepository_throwsWhenDeletingOtherUsersRepository() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repository.getOwnerType()).thenReturn(OwnerType.USER);
        when(repository.getOwnerId()).thenReturn(OwnerId.of(10L));
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(20L));

        assertThrows(JgitkinsException.class, () -> service.deleteRepository(1L));

        verify(repositoryGitPort, never()).deleteRepository(any(), any());
        verify(repositoryPort, never()).deleteById(any(RepositoryId.class));
    }

    @Test
    void deleteRepository_deletesWhenAccessible() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(repository.getOwnerType()).thenReturn(OwnerType.ORGANIZATION);
        when(repository.getName()).thenReturn(RepositoryName.from("sample-repo"));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("team-a");

        service.deleteRepository(1L);

        verify(repositoryGitPort).deleteRepository("team-a", "sample-repo");
        verify(repositoryPort).deleteById(RepositoryId.of(1L));
    }

    @Test
    void getRepositoriesByUsername_excludesPrivateWhenRequesterIsDifferentUser() {
        Repository publicRepo = org.mockito.Mockito.mock(Repository.class);
        Repository privateRepo = org.mockito.Mockito.mock(Repository.class);

        when(userPort.findUserIdByUsername("alice")).thenReturn(Optional.of(7L));
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(9L));
        when(repositoryPort.findAllByOwner(OwnerType.USER, OwnerId.of(7L))).thenReturn(List.of(publicRepo, privateRepo));
        when(publicRepo.getVisibility()).thenReturn(RepositoryVisibility.PUBLIC);
        when(privateRepo.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(repositoryApplicationMapper.toDto(publicRepo))
                .thenReturn(RepositoryResult.builder().id(1L).name("public").build());

        List<RepositoryResult> response = service.getRepositoriesByUsername("alice");

        assertEquals(1, response.size());
        assertEquals("public", response.get(0).getName());
        verify(repositoryApplicationMapper, never()).toDto(privateRepo);
    }
}
