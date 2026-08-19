package io.jgitkins.server.identity.access.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.contract.result.UserQueryResult;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.mapper.UserApplicationMapper;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock private UserQueryPort userQueryPort;
    @Mock private UserRepository userRepository;
    @Mock private UserIdentityPersistencePort userIdentityPort;
    private final UserApplicationMapper userApplicationMapper = Mappers.getMapper(UserApplicationMapper.class);
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() { adminUserService = new AdminUserService(userQueryPort, userRepository, userIdentityPort, userApplicationMapper); }

    @Test
    void getUsers_mapsQueryResultsToSummaries() {
        when(userQueryPort.findAll()).thenReturn(List.of(queryResult(1L, "alice", "ACTIVE")));
        List<UserAdminSummary> summaries = adminUserService.getUsers();
        assertEquals(1, summaries.size());
        assertEquals("alice", summaries.get(0).username());
        assertEquals("ACTIVE", summaries.get(0).status());
    }

    @Test
    void getUser_returnsDetailWithIdentities() {
        LocalDateTime now = LocalDateTime.now();
        when(userQueryPort.findUserDetailsById(2L)).thenReturn(Optional.of(queryResult(2L, "bob", "BLOCKED")));
        when(userIdentityPort.findAllByUserId(2L)).thenReturn(List.of());
        UserAdminDetail detail = adminUserService.getUser(2L);
        assertEquals(2L, detail.id());
        assertEquals("BLOCKED", detail.status());
        assertEquals(0, detail.identities().size());
    }

    @Test
    void updateUserStatus_updatesWhenValid() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.rehydrate(3L, "carol", "carol@example.com", "Carol", null, UserAuthority.USER,
                UserStatus.ACTIVE, now, now, now);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        adminUserService.updateUserStatus(3L, "blocked");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_throwsWhenStatusInvalid() {
        assertThrows(IllegalArgumentException.class, () -> adminUserService.updateUserStatus(1L, "UNKNOWN"));
    }

    private UserQueryResult queryResult(Long id, String username, String status) {
        LocalDateTime now = LocalDateTime.now();
        return new UserQueryResult(id, username, username + "@example.com", username, null, status, now, now, now);
    }
}
