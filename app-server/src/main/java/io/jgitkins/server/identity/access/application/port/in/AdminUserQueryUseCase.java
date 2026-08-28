package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.dto.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminSummary;
import java.util.List;

public interface AdminUserQueryUseCase {
    List<UserAdminSummary> getUsers(Long requesterUserId);
    UserAdminDetail getUser(Long requesterUserId, Long userId);
}
