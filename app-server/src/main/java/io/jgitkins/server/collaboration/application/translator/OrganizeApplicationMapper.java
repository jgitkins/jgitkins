package io.jgitkins.server.collaboration.application.translator;

import io.jgitkins.server.collaboration.application.contract.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizeApplicationMapper {

    @Mapping(target = "id", expression = "java(organize.getId() != null ? organize.getId().getValue() : null)")
    @Mapping(target = "name", expression = "java(organize.getName().getValue())")
    @Mapping(target = "ownerId", expression = "java(organize.getOwnerId() != null ? organize.getOwnerId().getValue() : null)")
    OrganizeCreationResult toDto(Organize organize);
}
