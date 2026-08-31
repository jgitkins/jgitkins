package io.jgitkins.server.identity.access.adapter.in.rest.mapper;

import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.adapter.in.rest.dto.request.OAuthLoginRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OAuthRequestMapper {

    @Mapping(target = "provider", source = "request.provider")
    @Mapping(target = "idToken", source = "request.idToken")
    OAuthLoginCommand toCommand(OAuthLoginRequest request);
}
