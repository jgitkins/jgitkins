package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.dto.UserSummary;
import io.jgitkins.web.application.dto.UsernameUpdateResult;
import java.util.List;

public interface UserPort {
	List<UserSummary> fetchUsers();

	UsernameUpdateResult updateUsername(String username);
}
