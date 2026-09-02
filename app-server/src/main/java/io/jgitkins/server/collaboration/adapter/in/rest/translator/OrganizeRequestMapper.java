package io.jgitkins.server.collaboration.adapter.in.rest.translator;

import io.jgitkins.server.collaboration.application.contract.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.adapter.in.rest.contract.request.OrganizeCreationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizeRequestMapper {

    @Mapping(target = "requesterUserId", source = "requesterUserId")
    OrganizeCreationCommand toCommand(OrganizeCreationRequest request, Long requesterUserId);
}
