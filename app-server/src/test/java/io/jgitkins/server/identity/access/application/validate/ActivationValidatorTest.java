package io.jgitkins.server.identity.access.application.validate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivationValidatorTest {

    @Mock
    private UserPersistencePort userPort;

    @Mock
    private OrganizePersistencePort organizePort;

    @Mock
    private RepositoryQueryPort repositoryQueryPort;

    @InjectMocks
    private ActivationValidator validator;

    @Test
    void validateUsername_throwsWhenInvalid() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateUsername("bad name"));
    }

    @Test
    void validateUserHasNoRepositories_throwsWhenOwnedRepositoriesExist() {
        when(repositoryQueryPort.countByOwner(OwnerType.USER, OwnerId.of(1L))).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> validator.validateUserHasNoRepositories(1L));
    }
}
