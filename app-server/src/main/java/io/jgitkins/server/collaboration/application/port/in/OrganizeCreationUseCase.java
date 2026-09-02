package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.contract.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.contract.OrganizeCreationResult;

public interface OrganizeCreationUseCase {
    OrganizeCreationResult createOrganize(OrganizeCreationCommand command);
}
