package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.BranchEntity;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntityCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BranchEntityMbgMapper {
    long countByCondition(BranchEntityCondition example);

    int deleteByCondition(BranchEntityCondition example);

    int deleteByPrimaryKey(Long id);

    int insert(BranchEntity row);

    int insertSelective(BranchEntity row);

    List<BranchEntity> selectByCondition(BranchEntityCondition example);

    BranchEntity selectByPrimaryKey(Long id);

    int updateByConditionSelective(@Param("row") BranchEntity row, @Param("example") BranchEntityCondition example);

    int updateByCondition(@Param("row") BranchEntity row, @Param("example") BranchEntityCondition example);

    int updateByPrimaryKeySelective(BranchEntity row);

    int updateByPrimaryKey(BranchEntity row);
}