package io.jgitkins.server.change.review.adapter.out.persistence.mapper;

import io.jgitkins.server.change.review.adapter.out.persistence.model.PullRequestEntity;
import io.jgitkins.server.change.review.adapter.out.persistence.model.PullRequestEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PullRequestEntityMbgMapper {
    long countByCondition(PullRequestEntityCondition example);

    int deleteByCondition(PullRequestEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(PullRequestEntity row);

    int insertSelective(PullRequestEntity row);

    List<PullRequestEntity> selectByCondition(PullRequestEntityCondition example);

    PullRequestEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") PullRequestEntity row, @Param("example") PullRequestEntityCondition example);

    int updateByCondition(@Param("row") PullRequestEntity row, @Param("example") PullRequestEntityCondition example);

    int updateByPrimaryKeySelective(PullRequestEntity row);

    int updateByPrimaryKey(PullRequestEntity row);
}
