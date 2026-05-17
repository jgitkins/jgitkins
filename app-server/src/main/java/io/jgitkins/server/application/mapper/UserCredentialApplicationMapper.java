package io.jgitkins.server.application.mapper;

import io.jgitkins.server.application.dto.result.UserCredentialSummary;
import io.jgitkins.server.domain.model.UserCredential;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCredentialApplicationMapper {

    UserCredentialSummary toSummary(UserCredential credential);
}
