package io.jgitkins.runner.infrastructure.persistence.mapper;

import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigFileEntity;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigFileEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunnerConfigFileEntityMbgMapper {
    long countByCondition(RunnerConfigFileEntityCondition example);

    int deleteByCondition(RunnerConfigFileEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RunnerConfigFileEntity row);

    int insertSelective(RunnerConfigFileEntity row);

    List<RunnerConfigFileEntity> selectByCondition(RunnerConfigFileEntityCondition example);

    RunnerConfigFileEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RunnerConfigFileEntity row, @Param("example") RunnerConfigFileEntityCondition example);

    int updateByCondition(@Param("row") RunnerConfigFileEntity row, @Param("example") RunnerConfigFileEntityCondition example);

    int updateByPrimaryKeySelective(RunnerConfigFileEntity row);

    int updateByPrimaryKey(RunnerConfigFileEntity row);
}