package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.contract.UserCredentialIssueRequest;
import io.jgitkins.web.application.contract.UserCredentialIssueResult;
import io.jgitkins.web.application.contract.UserCredentialSummary;
import java.util.List;

public interface UserCredentialPort {
	List<UserCredentialSummary> fetchPersonalAccessTokens();
	UserCredentialIssueResult issuePersonalAccessToken(UserCredentialIssueRequest request);
	void revokePersonalAccessToken(Long credentialId);
}
