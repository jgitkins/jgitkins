package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeMemberEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizeMemberEntityMbgMapper {
    long countByCondition(OrganizeMemberEntityCondition example);

    int deleteByCondition(OrganizeMemberEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(OrganizeMemberEntity row);

    int insertSelective(OrganizeMemberEntity row);

    List<OrganizeMemberEntity> selectByCondition(OrganizeMemberEntityCondition example);

    OrganizeMemberEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") OrganizeMemberEntity row, @Param("example") OrganizeMemberEntityCondition example);

    int updateByCondition(@Param("row") OrganizeMemberEntity row, @Param("example") OrganizeMemberEntityCondition example);

    int updateByPrimaryKeySelective(OrganizeMemberEntity row);

    int updateByPrimaryKey(OrganizeMemberEntity row);
}