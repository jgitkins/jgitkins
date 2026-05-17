package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.UserIdentitiesEntity;
import io.jgitkins.server.infrastructure.persistence.model.UserIdentitiesEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserIdentitiesEntityMbgMapper {
    long countByCondition(UserIdentitiesEntityCondition example);

    int deleteByCondition(UserIdentitiesEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(UserIdentitiesEntity row);

    int insertSelective(UserIdentitiesEntity row);

    List<UserIdentitiesEntity> selectByCondition(UserIdentitiesEntityCondition example);

    UserIdentitiesEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") UserIdentitiesEntity row, @Param("example") UserIdentitiesEntityCondition example);

    int updateByCondition(@Param("row") UserIdentitiesEntity row, @Param("example") UserIdentitiesEntityCondition example);

    int updateByPrimaryKeySelective(UserIdentitiesEntity row);

    int updateByPrimaryKey(UserIdentitiesEntity row);
}