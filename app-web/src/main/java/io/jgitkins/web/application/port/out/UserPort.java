package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.contract.UserSummary;
import io.jgitkins.web.application.contract.UsernameUpdateResult;
import java.util.List;

public interface UserPort {
	List<UserSummary> fetchUsers();

	UsernameUpdateResult updateUsername(String username);
}
