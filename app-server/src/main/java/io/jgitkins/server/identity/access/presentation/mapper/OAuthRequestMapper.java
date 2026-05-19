package io.jgitkins.server.identity.access.presentation.mapper;

import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.presentation.dto.OAuthLoginRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OAuthRequestMapper {

    @Mapping(target = "provider", source = "request.provider")
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "emailVerified", source = "request.emailVerified")
    @Mapping(target = "avatarUrl", source = "request.avatarUrl")
    OAuthLoginCommand toCommand(OAuthLoginRequest request);
}
