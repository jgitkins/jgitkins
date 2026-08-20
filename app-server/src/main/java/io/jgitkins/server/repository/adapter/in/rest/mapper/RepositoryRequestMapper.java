package io.jgitkins.server.repository.adapter.in.rest.mapper;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.command.UpdateRepositoryCommand;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.RepositoryCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.RepositoryUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RepositoryRequestMapper {

    @Mapping(source = "username", target = "authorName")
    @Mapping(source = "email", target = "authorEmail")
    @Mapping(target = "ownerType", expression = "java(toOwnerType(request.ownerType()))")
    @Mapping(target = "visibility", expression = "java(toVisibility(request.visibility()))")
    RepositoryCreateCommand toCommand(RepositoryCreateRequest request);

    UpdateRepositoryCommand toUpdateCommand(RepositoryUpdateRequest request);

    default OwnerType toOwnerType(String ownerType) {
        return ownerType == null ? null : OwnerType.from(ownerType);
    }

    default RepositoryVisibility toVisibility(String visibility) {
        return visibility == null || visibility.isBlank() ? null : RepositoryVisibility.from(visibility);
    }
}
