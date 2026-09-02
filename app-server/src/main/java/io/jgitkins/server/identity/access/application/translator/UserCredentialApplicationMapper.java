package io.jgitkins.server.identity.access.application.translator;

import io.jgitkins.server.identity.access.application.contract.UserCredentialSummary;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCredentialApplicationMapper {

    UserCredentialSummary toSummary(UserCredential credential);
}
