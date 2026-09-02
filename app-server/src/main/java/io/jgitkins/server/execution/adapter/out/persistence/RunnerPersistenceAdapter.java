package io.jgitkins.server.execution.adapter.out.persistence;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerAssignmentDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.translator.RunnerAssignmentEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.translator.RunnerEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.model.RunnerAssignmentEntity;
import io.jgitkins.server.execution.adapter.out.persistence.model.RunnerAssignmentEntityCondition;
import io.jgitkins.server.execution.adapter.out.persistence.model.RunnerEntity;
import io.jgitkins.server.execution.adapter.out.persistence.model.RunnerEntityCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registered by {@code ExecutionRunnerPersistenceSelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.74: with a JPA implementation of
 * {@code RunnerRepository} on the classpath, scanning would register two candidates and the injection
 * point would be ambiguous.
 */
@RequiredArgsConstructor
public class RunnerPersistenceAdapter implements RunnerRepository {

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
                // The requested scope, carried on an aggregate that now has the generated id. This used
                // to map the assignment from restoreRunner(entity), which reads the scope back out of a
                // database that has no assignment row yet -- so it returned the GLOBAL fallback and every
                // runner created here was recorded as GLOBAL no matter what scope the caller asked for.
                // The return value came from `runner`, so the caller saw the scope it requested and the
                // discrepancy stayed invisible. It could not be observed while the update branch wrote no
                // assignment row either: a runner was created GLOBAL and stayed GLOBAL. The JPA adapter
                // maps this from `runner` and always did; the two must not disagree about scope.
                Runner persisted = runnerDomainMapper.toDomain(
                        entity, runner.getScopeType(), runner.getScopeTargetId());
                runnerAssignmentEntityMbgMapper.insertSelective(runnerAssignmentDomainMapper.toEntity(persisted));
                return persisted;

            } else {
                runnerEntityMbgMapper.updateByPrimaryKeySelective(entity);
                appendAssignmentIfScopeChanged(runner);
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

    /**
     * Records a new effective scope, and only a new one.
     *
     * <p>This branch used to call {@code updateByPrimaryKeySelective} with an entity whose id
     * {@code RunnerAssignmentDomainMapper} never populates, so the statement resolved to
     * {@code where ID = null} and changed no row. Scope changes had never taken effect, and a
     * narrowed scope looked applied while the runner kept receiving everything it had before.
     *
     * <p>Append rather than update in place: reads already take the newest row,
     * {@code RUNNER_ASSIGNMENT} carries no unique key on {@code RUNNER_ID}, and an in-place update
     * would first have to look up the newest row's id. Appending also leaves an audit trail.
     *
     * <p>Only when the value differs. {@code activate} comes down this branch on every runner restart
     * carrying the scope through unchanged, so writing unconditionally would make this table a
     * restart log rather than a scope history.
     *
     * <p>The JPA adapter does the same thing. They are two implementations of one security-relevant
     * decision and they must not disagree about it.
     */
    private void appendAssignmentIfScopeChanged(Runner runner) {
        RunnerAssignmentEntity current = fetchAssignment(runner.getId());
        String targetType = runner.getScopeType().name();
        Long targetId = runner.getScopeType().requiresTargetId() ? runner.getScopeTargetId() : null;
        if (current != null
                && targetType.equals(current.getTargetType())
                && Objects.equals(targetId, current.getTargetId())) {
            return;
        }
        runnerAssignmentEntityMbgMapper.insertSelective(runnerAssignmentDomainMapper.toEntity(runner));
    }

    private RunnerAssignmentEntity fetchAssignment(Long runnerId) {
        RunnerAssignmentEntityCondition condition = new RunnerAssignmentEntityCondition();
        condition.createCriteria().andRunnerIdEqualTo(runnerId);
        // id desc breaks the tie. ASSIGNED_AT is a whole-second timestamp and the mapper fills it with
        // LocalDateTime.now(), so two rows written in the same second are indistinguishable by it.
        //
        // limit 1 in the order-by string is this file's existing idiom (findByToken above) and it is
        // load-bearing now: MBG emits `order by ${orderByClause}` and nothing else, so without it this
        // selected every assignment row for the runner and discarded all but the first in Java. The JPA
        // sibling emits LIMIT 1 through findFirst..., so the two providers were fetching different
        // amounts for the same question.
        condition.setOrderByClause("assigned_at desc, id desc limit 1");
        List<RunnerAssignmentEntity> assignments = runnerAssignmentEntityMbgMapper.selectByCondition(condition);
        if (assignments.isEmpty()) {
            return null;
        }
        return assignments.get(0);
    }
}
