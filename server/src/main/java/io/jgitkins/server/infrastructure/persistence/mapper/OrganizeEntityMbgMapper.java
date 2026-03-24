package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.OrganizeEntity;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizeEntityMbgMapper {
    long countByCondition(OrganizeEntityCondition example);

    int deleteByCondition(OrganizeEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(OrganizeEntity row);

    int insertSelective(OrganizeEntity row);

    List<OrganizeEntity> selectByConditionWithBLOBs(OrganizeEntityCondition example);

    List<OrganizeEntity> selectByCondition(OrganizeEntityCondition example);

    OrganizeEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByConditionWithBLOBs(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByCondition(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByPrimaryKeySelective(OrganizeEntity row);

    int updateByPrimaryKeyWithBLOBs(OrganizeEntity row);

    int updateByPrimaryKey(OrganizeEntity row);
}