package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.RepositoryMemberEntity;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryMemberEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RepositoryMemberEntityMbgMapper {
    long countByCondition(RepositoryMemberEntityCondition example);

    int deleteByCondition(RepositoryMemberEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RepositoryMemberEntity row);

    int insertSelective(RepositoryMemberEntity row);

    List<RepositoryMemberEntity> selectByCondition(RepositoryMemberEntityCondition example);

    RepositoryMemberEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RepositoryMemberEntity row, @Param("example") RepositoryMemberEntityCondition example);

    int updateByCondition(@Param("row") RepositoryMemberEntity row, @Param("example") RepositoryMemberEntityCondition example);

    int updateByPrimaryKeySelective(RepositoryMemberEntity row);

    int updateByPrimaryKey(RepositoryMemberEntity row);
}