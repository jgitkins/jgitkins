package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.dto.UserCredentialIssueRequest;
import io.jgitkins.web.application.dto.UserCredentialIssueResult;

public interface PersonalAccessTokenIssueUseCase {
	UserCredentialIssueResult issueToken(UserCredentialIssueRequest request);
	void revokeToken(Long credentialId);
}
