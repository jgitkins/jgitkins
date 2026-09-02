package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.contract.UserCredentialIssueCommand;
import io.jgitkins.server.identity.access.application.contract.UserCredentialIssueResult;

public interface UserCredentialIssueUseCase {
    UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command);
}
