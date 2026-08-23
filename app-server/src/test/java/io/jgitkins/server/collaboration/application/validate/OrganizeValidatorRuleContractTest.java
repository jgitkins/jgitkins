package io.jgitkins.server.collaboration.application.validate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.exception.OrganizeAlreadyExistsException;
import io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeValidatorRuleContractTest {
    @Mock OrganizeRepository organizeRepository;
    @Mock OrganizeMembershipQueryPort organizeMembershipQueryPort;

    private final Organize organize = Organize.reconstruct(
            OrganizeId.of(10L), OrganizeName.from("team"), "description", OwnerId.of(7L),
            LocalDateTime.now(), LocalDateTime.now());

    @Test
    void duplicateCreationMapsRepositoryHitToAlreadyExists() {
        OrganizeName name = OrganizeName.from("team");
        when(organizeRepository.findByName(name)).thenReturn(Optional.of(organize));

        assertThrows(OrganizeAlreadyExistsException.class,
                () -> validator().validateCreation(name));
    }

    @Test
    void uniqueCreationDoesNotRaiseDuplicateException() {
        OrganizeName name = OrganizeName.from("new-team");
        when(organizeRepository.findByName(name)).thenReturn(Optional.empty());

        validator().validateCreation(name);
        verify(organizeRepository).findByName(name);
    }

    @Test
    void missingOrganizationMapsRepositoryMissToNotFound() {
        when(organizeRepository.findById(OrganizeId.of(404L))).thenReturn(Optional.empty());

        assertThrows(OrganizeNotFoundException.class,
                () -> validator().findByIdOrThrow(404L));
    }

    @Test
    void existingOrganizationIsReturnedByLookup() {
        when(organizeRepository.findById(OrganizeId.of(10L))).thenReturn(Optional.of(organize));

        assertTrue(validator().findByIdOrThrow(10L) == organize);
    }

    @Test
    void ownerAccessUsesCompatibilityProjectionWithoutMembershipLookup() {
        assertTrue(validator().isAccessible(organize, 7L));
        verify(organizeMembershipQueryPort, never()).findRoleByOrganizeIdAndUserId(10L, 7L);
    }

    @Test
    void memberAccessUsesMembershipQuery() {
        when(organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(10L, 8L))
                .thenReturn(Optional.of(OrganizeMemberRole.MEMBER));

        assertTrue(validator().isAccessible(organize, 8L));
    }

    @Test
    void nonMemberAccessIsDenied() {
        when(organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(10L, 9L))
                .thenReturn(Optional.empty());

        assertFalse(validator().isAccessible(organize, 9L));
    }

    @Test
    void nullRequesterIsDenied() {
        assertFalse(validator().isAccessible(organize, null));
        verify(organizeMembershipQueryPort, never()).findRoleByOrganizeIdAndUserId(10L, null);
    }

    private OrganizeValidator validator() {
        return new OrganizeValidator(organizeRepository, organizeMembershipQueryPort);
    }
}
