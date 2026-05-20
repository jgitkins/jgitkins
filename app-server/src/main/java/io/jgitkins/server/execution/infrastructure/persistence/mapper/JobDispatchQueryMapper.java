package io.jgitkins.server.execution.infrastructure.persistence.mapper;

import io.jgitkins.server.execution.infrastructure.persistence.model.DispatchableJobRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobDispatchQueryMapper {
    DispatchableJobRow selectNextDispatchableJob(@Param("dispatchScope") String dispatchScope,
                                                 @Param("scopeTargetId") Long scopeTargetId);
}
