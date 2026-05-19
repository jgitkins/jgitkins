package io.jgitkins.server.collaboration.presentation.mapper;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.command.UpdateOrganizeCommand;
import io.jgitkins.server.collaboration.presentation.dto.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.presentation.dto.OrganizeUpdateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizeRequestMapper {

    OrganizeCreationCommand toCommand(OrganizeCreationRequest request);

    UpdateOrganizeCommand toCommand(OrganizeUpdateRequest request);
}
