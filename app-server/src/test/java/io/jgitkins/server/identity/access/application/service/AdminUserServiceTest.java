package io.jgitkins.server.identity.access.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.contract.external.UserQueryResult;
import io.jgitkins.server.identity.access.application.exception.AdminPrivilegeRequiredException;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.identity.access.application.contract.UserAdminDetail;
import io.jgitkins.server.identity.access.application.contract.UserAdminSummary;
import io.jgitkins.server.identity.access.application.translator.UserApplicationMapper;
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

    private static final long ADMIN_ID = 900L;
    private static final long PLAIN_USER_ID = 901L;

    private void administratorIsSignedIn() {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(User.rehydrate(
                ADMIN_ID, "root", "root@example.com", "Root", null, UserAuthority.ADMIN,
                UserStatus.ACTIVE, now, now, now)));
    }

    // --- authorization -----------------------------------------------------------------------
    //
    // Added 2026-08-28. Every method on this service was reachable unauthenticated: the use case
    // signatures carried no requester, SecurityConfig is permitAll, and there is no method security.
    // An unauthenticated caller could set any account, administrators included, to BLOCKED or
    // DELETED. These are the tests that were missing, not merely failing.

    @Test
    void updateUserStatus_rejectsAnAnonymousCaller() {
        assertThrows(UnauthenticatedException.class,
                () -> adminUserService.updateUserStatus(null, 3L, "blocked"));
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_rejectsANonAdministrator() {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(PLAIN_USER_ID)).thenReturn(Optional.of(User.rehydrate(
                PLAIN_USER_ID, "mallory", "m@example.com", "Mallory", null, UserAuthority.USER,
                UserStatus.ACTIVE, now, now, now)));

        assertThrows(AdminPrivilegeRequiredException.class,
                () -> adminUserService.updateUserStatus(PLAIN_USER_ID, 3L, "blocked"));
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_rejectsARequesterIdThatNamesNobody() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        // A token carrying a deleted user's id must not act as an administrator.
        assertThrows(AdminPrivilegeRequiredException.class,
                () -> adminUserService.updateUserStatus(404L, 3L, "blocked"));
    }

    @Test
    void getUsers_rejectsANonAdministrator() {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(PLAIN_USER_ID)).thenReturn(Optional.of(User.rehydrate(
                PLAIN_USER_ID, "mallory", "m@example.com", "Mallory", null, UserAuthority.USER,
                UserStatus.ACTIVE, now, now, now)));

        // The list carries every user's email. It was readable without authenticating.
        assertThrows(AdminPrivilegeRequiredException.class,
                () -> adminUserService.getUsers(PLAIN_USER_ID));
    }

    @Test
    void getUser_rejectsANonAdministrator() {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(PLAIN_USER_ID)).thenReturn(Optional.of(User.rehydrate(
                PLAIN_USER_ID, "mallory", "m@example.com", "Mallory", null, UserAuthority.USER,
                UserStatus.ACTIVE, now, now, now)));

        assertThrows(AdminPrivilegeRequiredException.class,
                () -> adminUserService.getUser(PLAIN_USER_ID, 2L));
    }

    // --- behaviour, now behind the gate ------------------------------------------------------

    @Test
    void getUsers_mapsQueryResultsToSummaries() {
        administratorIsSignedIn();
        when(userQueryPort.findAll()).thenReturn(List.of(queryResult(1L, "alice", "ACTIVE")));
        List<UserAdminSummary> summaries = adminUserService.getUsers(ADMIN_ID);
        assertEquals(1, summaries.size());
        assertEquals("alice", summaries.get(0).username());
        assertEquals("ACTIVE", summaries.get(0).status());
    }

    @Test
    void getUser_returnsDetailWithIdentities() {
        LocalDateTime now = LocalDateTime.now();
        administratorIsSignedIn();
        when(userQueryPort.findUserDetailsById(2L)).thenReturn(Optional.of(queryResult(2L, "bob", "BLOCKED")));
        when(userIdentityPort.findAllByUserId(2L)).thenReturn(List.of());
        UserAdminDetail detail = adminUserService.getUser(ADMIN_ID, 2L);
        assertEquals(2L, detail.id());
        assertEquals("BLOCKED", detail.status());
        assertEquals(0, detail.identities().size());
    }

    @Test
    void updateUserStatus_updatesWhenValid() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.rehydrate(3L, "carol", "carol@example.com", "Carol", null, UserAuthority.USER,
                UserStatus.ACTIVE, now, now, now);
        administratorIsSignedIn();
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        adminUserService.updateUserStatus(ADMIN_ID, 3L, "blocked");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_throwsWhenStatusInvalid() {
        administratorIsSignedIn();
        assertThrows(IllegalArgumentException.class,
                () -> adminUserService.updateUserStatus(ADMIN_ID, 1L, "UNKNOWN"));
    }

    private UserQueryResult queryResult(Long id, String username, String status) {
        LocalDateTime now = LocalDateTime.now();
        return new UserQueryResult(id, username, username + "@example.com", username, null, status, now, now, now);
    }
}
