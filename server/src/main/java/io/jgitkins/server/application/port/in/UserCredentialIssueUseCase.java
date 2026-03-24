package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.UserCredentialIssueCommand;
import io.jgitkins.server.application.dto.result.UserCredentialIssueResult;

public interface UserCredentialIssueUseCase {
    UserCredentialIssueResult issueCredential(UserCredentialIssueCommand command);
}
