package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.UserCredentialIssueRequest;
import io.jgitkins.web.application.dto.UserCredentialIssueResult;
import io.jgitkins.web.application.dto.UserCredentialSummary;
import java.util.List;

public interface SettingsFacadeUseCase {

    List<UserCredentialSummary> getPersonalAccessTokens();

    UserCredentialIssueResult issueToken(UserCredentialIssueRequest request);

    void revokeToken(Long credentialId);
}
