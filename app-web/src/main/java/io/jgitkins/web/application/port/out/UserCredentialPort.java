package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.dto.UserCredentialIssueRequest;
import io.jgitkins.web.application.dto.UserCredentialIssueResult;
import io.jgitkins.web.application.dto.UserCredentialSummary;
import java.util.List;

public interface UserCredentialPort {
	List<UserCredentialSummary> fetchPersonalAccessTokens();
	UserCredentialIssueResult issuePersonalAccessToken(UserCredentialIssueRequest request);
	void revokePersonalAccessToken(Long credentialId);
}
