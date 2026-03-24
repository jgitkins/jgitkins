package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeEntity;
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
                entity.getOwnerId() != null ? UserId.of(entity.getOwnerId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
