package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.UserCredentialIssueRequest;
import io.jgitkins.web.application.contract.UserCredentialIssueResult;
import io.jgitkins.web.application.contract.UserCredentialSummary;
import java.util.List;

public interface SettingsFacadeUseCase {

    List<UserCredentialSummary> getPersonalAccessTokens();

    UserCredentialIssueResult issueToken(UserCredentialIssueRequest request);

    void revokeToken(Long credentialId);
}
