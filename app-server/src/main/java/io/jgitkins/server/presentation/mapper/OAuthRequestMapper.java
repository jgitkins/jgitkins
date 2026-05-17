package io.jgitkins.server.presentation.mapper;

import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.presentation.dto.OAuthLoginRequest;
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
