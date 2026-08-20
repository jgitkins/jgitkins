package io.jgitkins.server.identity.access.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.infrastructure.mapper.UserDomainMapper;
import io.jgitkins.server.identity.access.infrastructure.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.identity.access.infrastructure.persistence.model.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterForUpdateTest {
    @Mock UserEntityMbgMapper mapper;
    @Mock UserDomainMapper domainMapper;

    @Test void delegatesToLockedMapperAndMapsResult() {
        UserEntity entity = new UserEntity();
        User user = User.create("user", "user@example.test", "User", null);
        when(mapper.selectByPrimaryKeyForUpdate(7L)).thenReturn(entity);
        when(domainMapper.toDomain(entity)).thenReturn(user);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(mapper, domainMapper);
        assertEquals(Optional.of(user), adapter.findByIdForUpdate(7L));
        verify(mapper).selectByPrimaryKeyForUpdate(7L);
    }

    @Test void nullMapperResultReturnsEmpty() {
        when(mapper.selectByPrimaryKeyForUpdate(7L)).thenReturn(null);
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(mapper, domainMapper);
        assertEquals(Optional.empty(), adapter.findByIdForUpdate(7L));
    }

    @Test void mapperFailureUsesPersistenceException() {
        when(mapper.selectByPrimaryKeyForUpdate(7L)).thenThrow(new RuntimeException("db"));
        UserPersistenceAdapter adapter = new UserPersistenceAdapter(mapper, domainMapper);
        assertThrows(InfrastructureException.class, () -> adapter.findByIdForUpdate(7L));
    }
}
