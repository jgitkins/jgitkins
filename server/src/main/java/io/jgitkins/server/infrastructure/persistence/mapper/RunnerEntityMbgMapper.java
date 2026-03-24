package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.RunnerEntity;
import io.jgitkins.server.infrastructure.persistence.model.RunnerEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunnerEntityMbgMapper {
    long countByCondition(RunnerEntityCondition example);

    int deleteByCondition(RunnerEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RunnerEntity row);

    int insertSelective(RunnerEntity row);

    List<RunnerEntity> selectByCondition(RunnerEntityCondition example);

    RunnerEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RunnerEntity row, @Param("example") RunnerEntityCondition example);

    int updateByCondition(@Param("row") RunnerEntity row, @Param("example") RunnerEntityCondition example);

    int updateByPrimaryKeySelective(RunnerEntity row);

    int updateByPrimaryKey(RunnerEntity row);
}