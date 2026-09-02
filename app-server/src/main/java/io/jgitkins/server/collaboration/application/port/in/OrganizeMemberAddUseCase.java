package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.command.OrganizeMemberAddCommand;

public interface OrganizeMemberAddUseCase {

    void addOrganizeMember(OrganizeMemberAddCommand command);
}
