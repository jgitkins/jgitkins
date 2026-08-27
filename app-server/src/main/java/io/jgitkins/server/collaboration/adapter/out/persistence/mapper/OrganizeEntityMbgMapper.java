package io.jgitkins.server.collaboration.adapter.out.persistence.mapper;

import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeEntity;
import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeEntityCondition;
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

    OrganizeEntity selectByOrganizeIdForUpdate(@Param("organizeId") Long organizeId);

    int updateByConditionSelective(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByConditionWithBLOBs(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByCondition(@Param("row") OrganizeEntity row, @Param("example") OrganizeEntityCondition example);

    int updateByPrimaryKeySelective(OrganizeEntity row);

    int updateByPrimaryKeyWithBLOBs(OrganizeEntity row);

    int updateByPrimaryKey(OrganizeEntity row);
}
