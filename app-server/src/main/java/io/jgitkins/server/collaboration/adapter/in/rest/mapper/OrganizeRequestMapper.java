package io.jgitkins.server.collaboration.adapter.in.rest.mapper;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.command.UpdateOrganizeCommand;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizeRequestMapper {

    @Mapping(target = "requesterUserId", source = "requesterUserId")
    OrganizeCreationCommand toCommand(OrganizeCreationRequest request, Long requesterUserId);

    UpdateOrganizeCommand toCommand(OrganizeUpdateRequest request);
}
