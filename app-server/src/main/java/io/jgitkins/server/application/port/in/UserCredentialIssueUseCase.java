package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.identity.access.application.dto.result.UserCredentialIssueResult;

public interface UserCredentialIssueUseCase {
    UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command);
}
