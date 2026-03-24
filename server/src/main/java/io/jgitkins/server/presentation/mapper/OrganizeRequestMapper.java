package io.jgitkins.server.presentation.mapper;

import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.command.UpdateOrganizeCommand;
import io.jgitkins.server.presentation.dto.OrganizeCreationRequest;
import io.jgitkins.server.presentation.dto.OrganizeUpdateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizeRequestMapper {

    OrganizeCreationCommand toCommand(OrganizeCreationRequest request);

    UpdateOrganizeCommand toCommand(OrganizeUpdateRequest request);
}
