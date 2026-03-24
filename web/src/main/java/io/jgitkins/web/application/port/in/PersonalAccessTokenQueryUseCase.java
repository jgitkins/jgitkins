package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.UserCredentialSummary;
import java.util.List;

public interface PersonalAccessTokenQueryUseCase {
	List<UserCredentialSummary> fetchPersonalAccessTokens();
}
