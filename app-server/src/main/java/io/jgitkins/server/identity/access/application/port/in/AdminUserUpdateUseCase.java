package io.jgitkins.server.identity.access.application.port.in;

public interface AdminUserUpdateUseCase {
    void updateUserStatus(Long requesterUserId, Long userId, String status);
}
