package io.jgitkins.server.collaboration.adapter.in.rest.mapper;

import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizeMemberRequestMapper {

    @Mapping(target = "organizeId", source = "organizeId")
    @Mapping(target = "userId", source = "request.userId")
    @Mapping(target = "role", source = "request.role")
    @Mapping(target = "requesterUserId", source = "requesterUserId")
    OrganizeMemberAddCommand toCommand(Long organizeId, OrganizeMemberAddRequest request, Long requesterUserId);
}
