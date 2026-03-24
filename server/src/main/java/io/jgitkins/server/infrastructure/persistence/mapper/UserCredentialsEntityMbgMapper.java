package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.UserCredentialsEntity;
import io.jgitkins.server.infrastructure.persistence.model.UserCredentialsEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserCredentialsEntityMbgMapper {
    long countByCondition(UserCredentialsEntityCondition example);

    int deleteByCondition(UserCredentialsEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(UserCredentialsEntity row);

    int insertSelective(UserCredentialsEntity row);

    List<UserCredentialsEntity> selectByConditionWithBLOBs(UserCredentialsEntityCondition example);

    List<UserCredentialsEntity> selectByCondition(UserCredentialsEntityCondition example);

    UserCredentialsEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") UserCredentialsEntity row, @Param("example") UserCredentialsEntityCondition example);

    int updateByConditionWithBLOBs(@Param("row") UserCredentialsEntity row, @Param("example") UserCredentialsEntityCondition example);

    int updateByCondition(@Param("row") UserCredentialsEntity row, @Param("example") UserCredentialsEntityCondition example);

    int updateByPrimaryKeySelective(UserCredentialsEntity row);

    int updateByPrimaryKeyWithBLOBs(UserCredentialsEntity row);

    int updateByPrimaryKey(UserCredentialsEntity row);
}