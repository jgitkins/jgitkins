package io.jgitkins.server.repository.adapter.in.rest.translator;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.repository.adapter.in.rest.contract.request.RepositoryCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RepositoryRequestMapper {

    @Mapping(source = "request.username", target = "authorName")
    @Mapping(source = "request.email", target = "authorEmail")
    @Mapping(target = "ownerType", expression = "java(toOwnerType(request.ownerType()))")
    @Mapping(target = "visibility", expression = "java(toVisibility(request.visibility()))")
    @Mapping(target = "requesterUserId", source = "requesterUserId")
    RepositoryCreateCommand toCommand(Long requesterUserId, RepositoryCreateRequest request);

    default OwnerType toOwnerType(String ownerType) {
        return ownerType == null ? null : OwnerType.from(ownerType);
    }

    default RepositoryVisibility toVisibility(String visibility) {
        return visibility == null || visibility.isBlank() ? null : RepositoryVisibility.from(visibility);
    }
}
