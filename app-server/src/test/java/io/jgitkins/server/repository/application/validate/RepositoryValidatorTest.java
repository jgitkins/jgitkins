package io.jgitkins.server.repository.application.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
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

    private RepositoryValidator validator() {
        return new RepositoryValidator(repositoryRepository, organizationMembershipPort);
    }

    @Test
    void organizationOwnerRequiresMembershipRolePresence() {
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L))
                .thenReturn(Optional.of(OrganizationMembershipRole.MEMBER));

        assertDoesNotThrow(() -> validator().validateOwnership(7L, OwnerType.ORGANIZATION, 10L));
    }

    @Test
    void organizationOwnerWithoutRoleIsDenied() {
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> validator().validateOwnership(7L, OwnerType.ORGANIZATION, 10L));
    }

    @Test
    void requireUserOwner_usesExplicitRequesterId() {

        assertDoesNotThrow(() -> validator().validateCreation(7L,
                OwnerType.USER, null, RepositoryName.from("repo")));
    }

    @Test
    void requireUserOwner_rejectsANullRequesterId() {
        // Task 2.64 replaced an ambient lookup with an argument. The rejection has to survive that: a
        // validator that accepted null would authorize a user-owned create for nobody in particular.
        assertThrows(RuntimeException.class, () -> validator().validateCreation(null,
                OwnerType.USER, null, RepositoryName.from("repo")));
        assertThrows(RuntimeException.class,
                () -> validator().validateOwnership(null, OwnerType.ORGANIZATION, 10L));
    }
}
