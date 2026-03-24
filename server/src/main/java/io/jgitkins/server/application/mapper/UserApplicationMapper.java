package io.jgitkins.server.application.mapper;

import io.jgitkins.server.application.dto.result.UserAdminDetail;
import io.jgitkins.server.application.dto.result.UserAdminSummary;
import io.jgitkins.server.application.dto.result.UserIdentitySummary;
import io.jgitkins.server.application.dto.result.UserSummary;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserIdentity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserApplicationMapper {

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserAdminSummary toAdminSummary(User user);

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "identities", source = "identities")
    UserAdminDetail toAdminDetail(User user, List<UserIdentitySummary> identities);

    UserIdentitySummary toIdentitySummary(UserIdentity identity);

    UserSummary toUserSummary(User user);
}
