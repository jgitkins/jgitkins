package io.jgitkins.server.identity.access.application.mapper;

import io.jgitkins.server.identity.access.application.contract.result.UserCredentialSummary;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCredentialApplicationMapper {

    UserCredentialSummary toSummary(UserCredential credential);
}
