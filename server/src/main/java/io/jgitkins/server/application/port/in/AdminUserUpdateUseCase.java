package io.jgitkins.server.application.port.in;

public interface AdminUserUpdateUseCase {
    void updateUserStatus(Long userId, String status);
}
