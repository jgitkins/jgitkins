package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RepositoryDomainMapper {

    @Mapping(target = "id", expression = "java(repository.getId() != null ? repository.getId().getValue() : null)")
    @Mapping(target = "name", expression = "java(repository.getName().getValue())")
    @Mapping(target = "path", expression = "java(repository.getPath().getValue())")
    @Mapping(target = "defaultBranch", expression = "java(repository.getDefaultBranch().getValue())")
    @Mapping(target = "visibility", expression = "java(repository.getVisibility().name())")
    @Mapping(target = "ownerId", expression = "java(repository.getOwnerId() != null ? repository.getOwnerId().getValue() : null)")
    @Mapping(target = "ownerType", expression = "java(repository.getOwnerType() != null ? repository.getOwnerType().name() : null)")
    @Mapping(target = "status", expression = "java(repository.getLastSyncedAt() != null ? \"ACTIVE\" : \"REGISTERED\")")
    @Mapping(target = "clonePath", expression = "java(repository.getClonePath())")
    @Mapping(target = "credentialId", expression = "java(repository.getCredentialId())")
    @Mapping(target = "description", expression = "java(repository.getDescription())")
    @Mapping(target = "lastSyncedAt", expression = "java(repository.getLastSyncedAt())")
    @Mapping(target = "createdAt", expression = "java(repository.getCreatedAt())")
    @Mapping(target = "updatedAt", expression = "java(repository.getUpdatedAt())")
    RepositoryEntity toEntity(Repository repository);

    default Repository toDomain(RepositoryEntity entity) {
        OwnerType ownerType = OwnerType.from(entity.getOwnerType());
        OwnerId ownerId = entity.getOwnerId() != null ? OwnerId.of(entity.getOwnerId()) : null;
        return Repository.rehydrate(
                RepositoryId.of(entity.getId()),
                ownerType,
                ownerId,
                RepositoryName.from(entity.getName()),
                RepositoryPath.from(entity.getPath()),
                BranchName.of(entity.getDefaultBranch()),
                RepositoryVisibility.from(entity.getVisibility()),
                entity.getDescription(),
                entity.getClonePath(),
                entity.getCredentialId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastSyncedAt()
        );
    }
}
