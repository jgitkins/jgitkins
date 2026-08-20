package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.dto.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.dto.result.UserIdentitySummary;
import io.jgitkins.server.identity.access.application.mapper.UserApplicationMapper;
import io.jgitkins.server.identity.access.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.AdminUserUpdateUseCase;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.application.contract.result.UserQueryResult;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
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
    public List<UserAdminSummary> getUsers() {
        return userQueryPort.findAll()
                .stream()
                .map(userApplicationMapper::toAdminSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminDetail getUser(Long userId) {
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
    public void updateUserStatus(Long userId, String status) {

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
}
