package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.OrganizeMemberAddCommand;

public interface OrganizeMemberAddUseCase {

    void addOrganizeMember(OrganizeMemberAddCommand command);
}
