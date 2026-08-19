package io.jgitkins.server.collaboration.adapter.in.rest.mapper;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.command.UpdateOrganizeCommand;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeUpdateRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizeRequestMapper {

    OrganizeCreationCommand toCommand(OrganizeCreationRequest request);

    UpdateOrganizeCommand toCommand(OrganizeUpdateRequest request);
}
