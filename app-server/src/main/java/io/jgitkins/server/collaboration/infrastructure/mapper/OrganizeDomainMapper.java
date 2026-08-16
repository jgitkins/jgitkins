package io.jgitkins.server.collaboration.infrastructure.mapper;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizeDomainMapper {

    @Mapping(target = "id", expression = "java(organize.getId() != null ? organize.getId().getValue() : null)")
    @Mapping(target = "name", expression = "java(organize.getName().getValue())")
    @Mapping(target = "path", expression = "java(organize.getName().getValue())")
    @Mapping(target = "ownerId", expression = "java(organize.getOwnerId() != null ? organize.getOwnerId().getValue() : null)")
    @Mapping(target = "description", expression = "java(organize.getDescription())")
    @Mapping(target = "createdAt", expression = "java(organize.getCreatedAt())")
    @Mapping(target = "updatedAt", expression = "java(organize.getUpdatedAt())")
    OrganizeEntity toEntity(Organize organize);

    default Organize toDomain(OrganizeEntity entity) {
        if (entity == null) {
            return null;
        }
        return Organize.reconstruct(
                entity.getId() != null ? OrganizeId.of(entity.getId()) : null,
                OrganizeName.from(entity.getName()),
                entity.getDescription(),
                entity.getOwnerId() != null ? OwnerId.of(entity.getOwnerId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
