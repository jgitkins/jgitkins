package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.dto.result.UserSummary;
import java.util.List;

public interface PublicUserQueryUseCase {
    List<UserSummary> getUsers();
}
