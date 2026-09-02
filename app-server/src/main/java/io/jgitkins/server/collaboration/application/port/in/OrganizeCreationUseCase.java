package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.contract.result.OrganizeCreationResult;

public interface OrganizeCreationUseCase {
    OrganizeCreationResult createOrganize(OrganizeCreationCommand command);
}
