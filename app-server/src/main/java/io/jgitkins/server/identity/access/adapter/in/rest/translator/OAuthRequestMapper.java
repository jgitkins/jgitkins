package io.jgitkins.server.identity.access.adapter.in.rest.translator;

import io.jgitkins.server.identity.access.application.contract.OAuthLoginCommand;
import io.jgitkins.server.identity.access.adapter.in.rest.contract.request.OAuthLoginRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OAuthRequestMapper {

    @Mapping(target = "provider", source = "request.provider")
    @Mapping(target = "idToken", source = "request.idToken")
    OAuthLoginCommand toCommand(OAuthLoginRequest request);
}
