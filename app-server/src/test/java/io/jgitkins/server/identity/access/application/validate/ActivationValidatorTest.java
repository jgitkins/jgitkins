package io.jgitkins.server.identity.access.application.validate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.port.out.OrganizationNameUniquenessPort;
import io.jgitkins.server.identity.access.application.port.out.OwnedRepositoryCountPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivationValidatorTest {
    @Mock private UserRepository userRepository;
    @Mock private OrganizationNameUniquenessPort organizationNameUniquenessPort;
    @Mock private OwnedRepositoryCountPort ownedRepositoryCountPort;
    @InjectMocks private ActivationValidator validator;

    @Test
    void validateUsername_throwsWhenInvalid() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateUsername("bad name"));
    }

    @Test
    void validateUserHasNoRepositories_throwsWhenOwnedRepositoriesExist() {
        when(ownedRepositoryCountPort.countByUserId(1L)).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> validator.validateUserHasNoRepositories(1L));
    }
}
