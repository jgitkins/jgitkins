package io.jgitkins.server.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCredentialIssueResult {
    private final Long credentialId;
    private final String token;
}
