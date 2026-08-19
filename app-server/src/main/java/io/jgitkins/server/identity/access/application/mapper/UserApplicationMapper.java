package io.jgitkins.server.identity.access.application.mapper;

import io.jgitkins.server.identity.access.application.dto.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.dto.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.dto.result.UserIdentitySummary;
import io.jgitkins.server.identity.access.application.dto.result.UserSummary;
import io.jgitkins.server.identity.access.application.contract.result.UserQueryResult;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserApplicationMapper {

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserAdminSummary toAdminSummary(User user);

    UserAdminSummary toAdminSummary(UserQueryResult user);

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "identities", source = "identities")
    UserAdminDetail toAdminDetail(User user, List<UserIdentitySummary> identities);

    @Mapping(target = "identities", source = "identities")
    UserAdminDetail toAdminDetail(UserQueryResult user, List<UserIdentitySummary> identities);

    UserIdentitySummary toIdentitySummary(UserIdentity identity);

    UserSummary toUserSummary(User user);

    UserSummary toUserSummary(UserQueryResult user);
}
