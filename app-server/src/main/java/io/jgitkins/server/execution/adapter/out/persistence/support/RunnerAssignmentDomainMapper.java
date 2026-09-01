package io.jgitkins.server.execution.adapter.out.persistence.support;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.adapter.out.persistence.model.RunnerAssignmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Builds the row that records a runner's effective scope.
 *
 * <p>{@code id} is deliberately unmapped, and now that is correct rather than a defect. The column is
 * auto-increment and every write through this mapper is an insert. It used to feed
 * {@code updateByPrimaryKeySelective} as well, where the unset id made the statement resolve to
 * {@code where ID = null} and silently change nothing -- scope updates had never taken effect. That
 * call site is gone; both adapters append.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RunnerAssignmentDomainMapper {

    @Mapping(target = "runnerId", expression = "java(runner.getId())")
    @Mapping(target = "targetType", expression = "java(runner.getScopeType().name())")
    @Mapping(target = "targetId", expression = "java(runner.getScopeType().requiresTargetId() ? runner.getScopeTargetId() : null)")
    @Mapping(target = "assignedAt", expression = "java(java.time.LocalDateTime.now())")
    RunnerAssignmentEntity toEntity(Runner runner);
}
