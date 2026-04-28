package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.shared.application.support.RepositoryAccessibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryLoadServiceTest {

    @Mock
    private RepositoryApplicationMapper repositoryApplicationMapper;
    @Mock
    private RepositoryPersistencePort repositoryPort;
    @Mock
    private CurrentUserPort currentUserPersistencePort;
    @Mock
    private UserPersistencePort userPort;
    @Mock
    private OrganizePersistencePort organizePort;
    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;

    private RepositoryLoadService service;

    @BeforeEach
    void setUp() {
        RepositoryLookupService lookupService = new RepositoryLookupService(repositoryPort, userPort, organizePort);
        RepositoryAccessibilityService accessibilityService = new RepositoryAccessibilityService(organizeMemberPort);
        service = new RepositoryLoadService(
                repositoryApplicationMapper,
                accessibilityService,
                lookupService,
                repositoryPort,
                currentUserPersistencePort,
                userPort
        );
    }

    @Test
    void loadRepository_returnsMappedResult() {
        Repository repository = org.mockito.Mockito.mock(Repository.class);
        when(repositoryPort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        RepositoryResult result = new RepositoryResult(1L, null, null, null, null, null, null, null, null, null, null, false, null, null, null);
        when(repositoryApplicationMapper.toDto(repository)).thenReturn(result);

        RepositoryResult response = service.loadRepository(1L);

        assertEquals(1L, response.id());
    }

    @Test
    void loadRepositories_returnsOnlyVisibleRepositoriesForRequester() {
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

        when(repositoryApplicationMapper.toDto(publicRepo)).thenReturn(new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null));
        when(repositoryApplicationMapper.toDto(myPrivateRepo)).thenReturn(new RepositoryResult(2L, null, "mine", null, null, null, null, null, null, null, null, false, null, null, null));
        when(repositoryApplicationMapper.toDto(orgPrivateRepo)).thenReturn(new RepositoryResult(3L, null, "org", null, null, null, null, null, null, null, null, false, null, null, null));

        List<RepositoryResult> response = service.loadRepositories();

        assertEquals(3, response.size());
        assertEquals(List.of("public", "mine", "org"), response.stream().map(RepositoryResult::name).toList());
    }

    @Test
    void loadUserRepositories_excludesPrivateWhenRequesterIsDifferentUser() {
        Repository publicRepo = org.mockito.Mockito.mock(Repository.class);
        Repository privateRepo = org.mockito.Mockito.mock(Repository.class);

        when(userPort.findUserIdByUsername("alice")).thenReturn(Optional.of(7L));
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(9L));
        when(repositoryPort.findAllByOwner(OwnerType.USER, OwnerId.of(7L))).thenReturn(List.of(publicRepo, privateRepo));
        when(publicRepo.getVisibility()).thenReturn(RepositoryVisibility.PUBLIC);
        when(privateRepo.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(repositoryApplicationMapper.toDto(publicRepo))
                .thenReturn(new RepositoryResult(1L, null, "public", null, null, null, null, null, null, null, null, false, null, null, null));

        List<RepositoryResult> response = service.loadUserRepositories("alice");

        assertEquals(1, response.size());
        assertEquals("public", response.get(0).name());
        org.mockito.Mockito.verify(repositoryApplicationMapper, never()).toDto(privateRepo);
    }
}
