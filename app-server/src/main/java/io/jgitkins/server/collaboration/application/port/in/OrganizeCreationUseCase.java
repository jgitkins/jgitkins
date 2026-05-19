package io.jgitkins.server.collaboration.application.port.in;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;

public interface OrganizeCreationUseCase {
    OrganizeCreationResult createOrganize(OrganizeCreationCommand command);
}
