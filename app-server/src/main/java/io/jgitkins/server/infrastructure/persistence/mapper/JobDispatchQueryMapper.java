package io.jgitkins.server.infrastructure.persistence.mapper;

import io.jgitkins.server.infrastructure.persistence.model.DispatchableJobRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JobDispatchQueryMapper {
    DispatchableJobRow selectNextDispatchableJob(@Param("dispatchScope") String dispatchScope,
                                                 @Param("scopeTargetId") Long scopeTargetId);
}
