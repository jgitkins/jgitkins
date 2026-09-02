package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.UserCredentialIssueRequest;
import io.jgitkins.web.application.contract.UserCredentialIssueResult;

public interface PersonalAccessTokenIssueUseCase {
	UserCredentialIssueResult issueToken(UserCredentialIssueRequest request);
	void revokeToken(Long credentialId);
}
