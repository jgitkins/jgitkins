package io.jgitkins.server.execution.adapter.out.persistence.translator;

import io.jgitkins.server.execution.adapter.out.persistence.model.DispatchableJobRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobDispatchQueryMapper {
    DispatchableJobRow selectNextDispatchableJob(@Param("dispatchScope") String dispatchScope,
                                                 @Param("scopeTargetId") Long scopeTargetId);
}
