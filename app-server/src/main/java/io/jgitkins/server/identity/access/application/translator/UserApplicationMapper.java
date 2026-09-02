package io.jgitkins.server.identity.access.application.translator;

import io.jgitkins.server.identity.access.application.contract.UserAdminDetail;
import io.jgitkins.server.identity.access.application.contract.UserAdminSummary;
import io.jgitkins.server.identity.access.application.contract.internal.UserIdentitySummary;
import io.jgitkins.server.identity.access.application.contract.UserSummary;
import io.jgitkins.server.identity.access.application.contract.external.UserQueryResult;
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
