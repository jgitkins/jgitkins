package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.UserSummary;
import java.util.List;

public interface PublicUserQueryUseCase {
    List<UserSummary> getUsers();
}
