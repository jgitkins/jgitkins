package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.UserCredentialSummary;
import java.util.List;

public interface PersonalAccessTokenQueryUseCase {
	List<UserCredentialSummary> fetchPersonalAccessTokens();
}
