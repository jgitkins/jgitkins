package io.jgitkins.runner.infrastructure.persistence.mapper;

import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigEntity;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunnerConfigEntityMbgMapper {
    long countByCondition(RunnerConfigEntityCondition example);

    int deleteByCondition(RunnerConfigEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RunnerConfigEntity row);

    int insertSelective(RunnerConfigEntity row);

    List<RunnerConfigEntity> selectByCondition(RunnerConfigEntityCondition example);

    RunnerConfigEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RunnerConfigEntity row, @Param("example") RunnerConfigEntityCondition example);

    int updateByCondition(@Param("row") RunnerConfigEntity row, @Param("example") RunnerConfigEntityCondition example);

    int updateByPrimaryKeySelective(RunnerConfigEntity row);

    int updateByPrimaryKey(RunnerConfigEntity row);
}