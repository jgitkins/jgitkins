package io.jgitkins.server.execution.adapter.out.persistence.translator;

import io.jgitkins.server.execution.adapter.out.persistence.model.JobHistoryEntity;
import io.jgitkins.server.execution.adapter.out.persistence.model.JobHistoryEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobHistoryEntityMbgMapper {
    long countByCondition(JobHistoryEntityCondition example);

    int deleteByCondition(JobHistoryEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(JobHistoryEntity row);

    int insertSelective(JobHistoryEntity row);

    List<JobHistoryEntity> selectByCondition(JobHistoryEntityCondition example);

    JobHistoryEntity selectLatestHistoryForUpdate(@Param("jobId") Long jobId);

    JobHistoryEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") JobHistoryEntity row, @Param("example") JobHistoryEntityCondition example);

    int updateByCondition(@Param("row") JobHistoryEntity row, @Param("example") JobHistoryEntityCondition example);

    int updateByPrimaryKeySelective(JobHistoryEntity row);

    int updateByPrimaryKey(JobHistoryEntity row);
}
