package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.contract.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.contract.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.internal.UserIdentitySummary;
import io.jgitkins.server.identity.access.application.translator.UserApplicationMapper;
import io.jgitkins.server.identity.access.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.AdminUserUpdateUseCase;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.application.internal.UserQueryResult;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.application.exception.AdminPrivilegeRequiredException;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService implements AdminUserQueryUseCase, AdminUserUpdateUseCase {

    private final UserQueryPort userQueryPort;
    private final UserRepository userRepository;
    private final UserIdentityPersistencePort userIdentityPort;
    private final UserApplicationMapper userApplicationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserAdminSummary> getUsers(Long requesterUserId) {
        requireAdministrator(requesterUserId);
        return userQueryPort.findAll()
                .stream()
                .map(userApplicationMapper::toAdminSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminDetail getUser(Long requesterUserId, Long userId) {
        requireAdministrator(requesterUserId);
        UserQueryResult user = userQueryPort.findUserDetailsById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")); // TODO: 도메인 예외 throw

        List<UserIdentitySummary> identities = userIdentityPort.findAllByUserId(userId)
                .stream()
                .map(userApplicationMapper::toIdentitySummary)
                .toList();

        return userApplicationMapper.toAdminDetail(user, identities);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long requesterUserId, Long userId, String status) {
        requireAdministrator(requesterUserId);


        // TODO: 상태에 대한 순수 유효성 검증은 API (@Valid) 단으로 이관
        UserStatus normalized = normalizeStatus(status);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")); // TODO: 도메인 예외 throw
        User updated = User.rehydrate(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getAuthority(),
                normalized,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
        userRepository.save(updated);
    }

    private UserStatus normalizeStatus(String status) {
        String normalized = status != null ? status.trim().toUpperCase(Locale.ROOT) : "";
        if ("PENDING_USERNAME".equals(normalized)) {
            return UserStatus.PENDING;
        }
        return UserStatus.fromString(normalized);
    }

    /**
     * Every method on this service is an administrator operation and every one of them was reachable
     * unauthenticated before this.
     *
     * <p>The use case signatures carried no requester at all, so no layer could have authorized:
     * the controller passed no principal, SecurityConfig is {@code anyRequest().permitAll()}, and
     * this application uses no method security. Anyone who could reach the port could set any
     * account, administrators included, to BLOCKED or DELETED, and could list every user.
     *
     * <p>The gate lives here rather than in the controller so a second inbound adapter cannot reach
     * the operation without it.
     */
    private void requireAdministrator(Long requesterUserId) {
        if (requesterUserId == null) {
            throw new UnauthenticatedException();
        }
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(AdminPrivilegeRequiredException::new);
        if (requester.getAuthority() != UserAuthority.ADMIN) {
            throw new AdminPrivilegeRequiredException();
        }
    }

}
