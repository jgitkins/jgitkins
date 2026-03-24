package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.OrganizeMemberAddCommand;

public interface OrganizeMemberAddUseCase {

    void addOrganizeMember(OrganizeMemberAddCommand command);
}
