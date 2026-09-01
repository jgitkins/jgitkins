package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.contract.command.UserCredentialIssueCommand;
import io.jgitkins.server.identity.access.application.contract.result.UserCredentialIssueResult;

public interface UserCredentialIssueUseCase {
    UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command);
}
