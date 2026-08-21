package io.jgitkins.server.repository.application.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryValidatorTest {
    @Mock RepositoryRepository repositoryRepository;
    @Mock OrganizationMembershipPort organizationMembershipPort;
    @Mock RepositoryActorPort repositoryActorPort;

    private RepositoryValidator validator() {
        return new RepositoryValidator(repositoryRepository, organizationMembershipPort, repositoryActorPort);
    }

    @Test
    void organizationOwnerRequiresMembershipRolePresence() {
        when(repositoryActorPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L))
                .thenReturn(Optional.of(OrganizationMembershipRole.MEMBER));

        assertDoesNotThrow(() -> validator().validateOwnership(OwnerType.ORGANIZATION, 10L));
    }

    @Test
    void organizationOwnerWithoutRoleIsDenied() {
        when(repositoryActorPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> validator().validateOwnership(OwnerType.ORGANIZATION, 10L));
    }

    @Test
    void userOwnerUsesRepositoryActorPort() {
        when(repositoryActorPort.resolveCurrentUserId()).thenReturn(Optional.of(7L));

        assertDoesNotThrow(() -> validator().validateCreation(
                OwnerType.USER, null, RepositoryName.from("repo")));
    }
}
