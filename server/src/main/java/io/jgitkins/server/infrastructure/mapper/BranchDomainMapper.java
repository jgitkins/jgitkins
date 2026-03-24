package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface BranchDomainMapper {

    BranchDomainMapper INSTANCE = Mappers.getMapper(BranchDomainMapper.class);

    // 도메인 모델 -> 영속성 모델 변환
    @Mapping(target = "isLocked", source = "locked")
    @Mapping(target = "isCi", source = "ciEnabled")
    @Mapping(target = "isDefault", source = "defaultBranch")
    BranchEntity toEntity(Branch branch);

    // 영속성 모델 -> 도메인 모델 변환
    default Branch toDomain(BranchEntity branchEntity) {
        if (branchEntity == null) {
            return null;
        }
        return Branch.rehydrate(
                branchEntity.getRepositoryId(),
                branchEntity.getName(),
                Boolean.TRUE.equals(branchEntity.getIsLocked()),
                Boolean.TRUE.equals(branchEntity.getIsCi()),
                Boolean.TRUE.equals(branchEntity.getIsDefault()));
    }
}
