package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.RunnerAssignmentEntity;
import io.jgitkins.server.infrastructure.persistence.model.RunnerAssignmentEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RunnerAssignmentEntityMbgMapper {
    long countByCondition(RunnerAssignmentEntityCondition example);

    int deleteByCondition(RunnerAssignmentEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(RunnerAssignmentEntity row);

    int insertSelective(RunnerAssignmentEntity row);

    List<RunnerAssignmentEntity> selectByCondition(RunnerAssignmentEntityCondition example);

    RunnerAssignmentEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") RunnerAssignmentEntity row, @Param("example") RunnerAssignmentEntityCondition example);

    int updateByCondition(@Param("row") RunnerAssignmentEntity row, @Param("example") RunnerAssignmentEntityCondition example);

    int updateByPrimaryKeySelective(RunnerAssignmentEntity row);

    int updateByPrimaryKey(RunnerAssignmentEntity row);
}