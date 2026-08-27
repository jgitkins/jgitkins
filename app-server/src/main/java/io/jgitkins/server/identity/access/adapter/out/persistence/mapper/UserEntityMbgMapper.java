package io.jgitkins.server.identity.access.adapter.out.persistence.mapper;

import io.jgitkins.server.identity.access.adapter.out.persistence.model.UserEntity;
import io.jgitkins.server.identity.access.adapter.out.persistence.model.UserEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserEntityMbgMapper {
    long countByCondition(UserEntityCondition example);

    int deleteByCondition(UserEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(UserEntity row);

    int insertSelective(UserEntity row);

    List<UserEntity> selectByCondition(UserEntityCondition example);

    UserEntity selectByPrimaryKey(Long id);

    UserEntity selectByPrimaryKeyForUpdate(Long id);

    int updateByConditionSelective(@Param("row") UserEntity row, @Param("example") UserEntityCondition example);

    int updateByCondition(@Param("row") UserEntity row, @Param("example") UserEntityCondition example);

    int updateByPrimaryKeySelective(UserEntity row);

    int updateByPrimaryKey(UserEntity row);
}