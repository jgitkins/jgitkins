package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.RunnerPersistencePort;
import io.jgitkins.server.domain.aggregate.Runner;
import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.RunnerAssignmentDomainMapper;
import io.jgitkins.server.infrastructure.mapper.RunnerDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.RunnerAssignmentEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.RunnerEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.RunnerAssignmentEntity;
import io.jgitkins.server.infrastructure.persistence.model.RunnerAssignmentEntityCondition;
import io.jgitkins.server.infrastructure.persistence.model.RunnerEntity;
import io.jgitkins.server.infrastructure.persistence.model.RunnerEntityCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RunnerPersistenceAdapter implements RunnerPersistencePort {

    private final RunnerEntityMbgMapper runnerEntityMbgMapper;
    private final RunnerAssignmentEntityMbgMapper runnerAssignmentEntityMbgMapper;

    private final RunnerDomainMapper runnerDomainMapper;
    private final RunnerAssignmentDomainMapper runnerAssignmentDomainMapper;

    @Override
    @Transactional
    public Runner save(Runner runner) {
        try {
            RunnerEntity entity = runnerDomainMapper.toEntity(runner);

            if (runner.getId() == null) {
                runnerEntityMbgMapper.insertSelective(entity);
                Runner restoredEntity = restoreRunner(entity);
                runnerAssignmentEntityMbgMapper.insertSelective(runnerAssignmentDomainMapper.toEntity(restoredEntity));
                return runnerDomainMapper.toDomain(entity, runner.getScopeType(), runner.getScopeTargetId());

            } else {
                runnerEntityMbgMapper.updateByPrimaryKeySelective(entity);
                runnerAssignmentEntityMbgMapper
                        .updateByPrimaryKeySelective(runnerAssignmentDomainMapper.toEntity(runner));
                RunnerEntity updated = runnerEntityMbgMapper.selectByPrimaryKey(runner.getId());
                return restoreRunner(updated);
            }
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save runner", e);
        }
    }

    @Override
    public void deleteById(Long runnerId) {
        try {
            deleteAssignment(runnerId);
            runnerEntityMbgMapper.deleteByPrimaryKey(runnerId);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete runner", e);
        }
    }

    @Override
    public Runner update(Runner runner) {
        try {
            RunnerEntity entity = runnerDomainMapper.toEntity(runner);
            runnerEntityMbgMapper.updateByPrimaryKeySelective(entity);
            RunnerEntity updated = runnerEntityMbgMapper.selectByPrimaryKey(runner.getId());
            return restoreRunner(updated);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during update runner", e);
        }
    }

    @Override
    public Optional<Runner> findById(Long runnerId) {
        try {
            RunnerEntity entity = runnerEntityMbgMapper.selectByPrimaryKey(runnerId);
            if (entity == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(restoreRunner(entity));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find runner by id", e);
        }
    }

    @Override
    public Optional<Runner> findByToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }
            RunnerEntityCondition condition = new RunnerEntityCondition();
            condition.createCriteria().andTokenEqualTo(token);
            condition.setOrderByClause("id DESC LIMIT 1");
            List<RunnerEntity> entities = runnerEntityMbgMapper.selectByCondition(condition);
            if (entities.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(restoreRunner(entities.get(0)));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find runner by token", e);
        }
    }

    @Override
    public List<Runner> findAll() {
        try {
            List<RunnerEntity> entities = runnerEntityMbgMapper.selectByCondition(new RunnerEntityCondition());
            return entities.stream()
                    .map(this::restoreRunner)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all runners", e);
        }
    }

    private void persistAssignment(Runner runner) {
        RunnerAssignmentEntity assignment = runnerAssignmentDomainMapper.toEntity(runner);
        runnerAssignmentEntityMbgMapper.insertSelective(assignment);
    }

    private void deleteAssignment(Long runnerId) {
        RunnerAssignmentEntityCondition condition = new RunnerAssignmentEntityCondition();
        condition.createCriteria().andRunnerIdEqualTo(runnerId);
        runnerAssignmentEntityMbgMapper.deleteByCondition(condition);
    }

    private Runner restoreRunner(RunnerEntity entity) {
        RunnerAssignmentEntity assignment = fetchAssignment(entity.getId());
        RunnerScopeType scopeType = assignment != null ? RunnerScopeType.valueOf(assignment.getTargetType())
                : RunnerScopeType.GLOBAL;
        Long targetId = assignment != null ? assignment.getTargetId() : null;
        return runnerDomainMapper.toDomain(entity, scopeType, targetId);
    }

    private RunnerAssignmentEntity fetchAssignment(Long runnerId) {
        RunnerAssignmentEntityCondition condition = new RunnerAssignmentEntityCondition();
        condition.createCriteria().andRunnerIdEqualTo(runnerId);
        condition.setOrderByClause("assigned_at desc");
        List<RunnerAssignmentEntity> assignments = runnerAssignmentEntityMbgMapper.selectByCondition(condition);
        if (assignments.isEmpty()) {
            return null;
        }
        return assignments.get(0);
    }
}
