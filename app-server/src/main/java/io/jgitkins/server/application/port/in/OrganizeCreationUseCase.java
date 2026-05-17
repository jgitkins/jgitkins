package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.result.OrganizeCreationResult;

public interface OrganizeCreationUseCase {
    OrganizeCreationResult createOrganize(OrganizeCreationCommand command);
}
