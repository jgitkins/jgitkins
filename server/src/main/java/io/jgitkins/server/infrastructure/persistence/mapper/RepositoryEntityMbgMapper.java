package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.RepositoryEntity;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepositoryEntityMbgMapper {
    long countByCondition(RepositoryEntityCondition example);

    int deleteByCondition(RepositoryEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RepositoryEntity row);

    int insertSelective(RepositoryEntity row);

    List<RepositoryEntity> selectByConditionWithBLOBs(RepositoryEntityCondition example);

    List<RepositoryEntity> selectByCondition(RepositoryEntityCondition example);

    RepositoryEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RepositoryEntity row, @Param("example") RepositoryEntityCondition example);

    int updateByConditionWithBLOBs(@Param("row") RepositoryEntity row, @Param("example") RepositoryEntityCondition example);

    int updateByCondition(@Param("row") RepositoryEntity row, @Param("example") RepositoryEntityCondition example);

    int updateByPrimaryKeySelective(RepositoryEntity row);

    int updateByPrimaryKeyWithBLOBs(RepositoryEntity row);

    int updateByPrimaryKey(RepositoryEntity row);
}