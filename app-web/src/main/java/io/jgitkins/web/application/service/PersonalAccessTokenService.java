package io.jgitkins.web.application.service;

import io.jgitkins.web.application.contract.UserCredentialIssueRequest;
import io.jgitkins.web.application.contract.UserCredentialIssueResult;
import io.jgitkins.web.application.contract.UserCredentialSummary;
import io.jgitkins.web.application.port.in.PersonalAccessTokenIssueUseCase;
import io.jgitkins.web.application.port.in.PersonalAccessTokenQueryUseCase;
import io.jgitkins.web.application.port.out.UserCredentialPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService implements PersonalAccessTokenQueryUseCase, PersonalAccessTokenIssueUseCase {

	private final UserCredentialPort userCredentialPort;

	@Override
	public List<UserCredentialSummary> fetchPersonalAccessTokens() {
		return userCredentialPort.fetchPersonalAccessTokens();
	}

	@Override
	public UserCredentialIssueResult issueToken(UserCredentialIssueRequest request) {
		return userCredentialPort.issuePersonalAccessToken(request);
	}

	@Override
	public void revokeToken(Long credentialId) {
		userCredentialPort.revokePersonalAccessToken(credentialId);
	}
}
