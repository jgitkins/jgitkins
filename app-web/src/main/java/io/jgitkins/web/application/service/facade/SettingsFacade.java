package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.contract.UserCredentialIssueRequest;
import io.jgitkins.web.application.contract.UserCredentialIssueResult;
import io.jgitkins.web.application.contract.UserCredentialSummary;
import io.jgitkins.web.application.port.in.PersonalAccessTokenIssueUseCase;
import io.jgitkins.web.application.port.in.PersonalAccessTokenQueryUseCase;
import io.jgitkins.web.application.port.in.facade.SettingsFacadeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsFacade implements SettingsFacadeUseCase {

    private final PersonalAccessTokenQueryUseCase personalAccessTokenQueryUseCase;
    private final PersonalAccessTokenIssueUseCase personalAccessTokenIssueUseCase;

    @Override
    public List<UserCredentialSummary> getPersonalAccessTokens() {
        return personalAccessTokenQueryUseCase.fetchPersonalAccessTokens();
    }

    @Override
    public UserCredentialIssueResult issueToken(UserCredentialIssueRequest request) {
        return personalAccessTokenIssueUseCase.issueToken(request);
    }

    @Override
    public void revokeToken(Long credentialId) {
        personalAccessTokenIssueUseCase.revokeToken(credentialId);
    }
}
