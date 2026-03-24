package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.JobEntity;
import io.jgitkins.server.infrastructure.persistence.model.JobEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobEntityMbgMapper {
    long countByCondition(JobEntityCondition example);

    int deleteByCondition(JobEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(JobEntity row);

    int insertSelective(JobEntity row);

    List<JobEntity> selectByCondition(JobEntityCondition example);

    JobEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") JobEntity row, @Param("example") JobEntityCondition example);

    int updateByCondition(@Param("row") JobEntity row, @Param("example") JobEntityCondition example);

    int updateByPrimaryKeySelective(JobEntity row);

    int updateByPrimaryKey(JobEntity row);
}