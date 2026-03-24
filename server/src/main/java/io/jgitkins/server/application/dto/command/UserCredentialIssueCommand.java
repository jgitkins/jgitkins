package io.jgitkins.server.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCredentialIssueCommand {
    private final String name;
    private final String description;
    private final String expiration;
}
