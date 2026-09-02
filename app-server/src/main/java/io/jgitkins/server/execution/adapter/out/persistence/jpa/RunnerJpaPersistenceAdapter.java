package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link RunnerRepository}.
 *
 * <p>A runner is stored across two tables: its own row, and an assignment row carrying the dispatch
 * scope. The effective scope is the newest assignment by {@code ASSIGNED_AT}, because
 * {@code RUNNER_ASSIGNMENT} has no unique key on {@code RUNNER_ID} and rows accumulate.
 *
 * <p><strong>The scope-update defect is fixed, in both providers at once.</strong> The MyBatis
 * adapter called {@code updateByPrimaryKeySelective} with an assignment entity whose id
 * {@code RunnerAssignmentDomainMapper} never populates, so the statement resolved to
 * {@code where ID = null} and changed nothing -- a runner's scope could not be changed, and
 * narrowing one for isolation looked applied while the runner kept receiving everything. This
 * adapter reproduced the no-op so that flipping the selector stayed invisible. Both then appended a new
 * assignment row when the scope differs from the newest one, which is what the read path was already
 * shaped for. Covered per provider, in the class that
 * builds that provider: {@code RunnerJpaMariaDbIntegrationTest#scopeUpdateTakesEffect} and
 * {@code RunnerScopeUpdateMariaDbTest#scopeUpdateTakesEffect}. No single method proves both -- a name
 * that says "under both providers" while constructing one is what this replaced.
 */
@Component
@RequiredArgsConstructor
public class RunnerJpaPersistenceAdapter implements RunnerRepository {

    private final RunnerJpaRepository runnerJpaRepository;
    private final RunnerAssignmentJpaRepository runnerAssignmentJpaRepository;

    @Override
    @Transactional
    public Runner save(Runner runner) {
        try {
            if (runner.getId() == null) {
                RunnerJpaEntity saved = runnerJpaRepository.save(toEntity(runner, null));
                runnerAssignmentJpaRepository.save(toAssignmentEntity(runner, saved.getId()));
                return toDomain(saved, runner.getScopeType(), runner.getScopeTargetId());
            }

            RunnerJpaEntity saved = runnerJpaRepository.save(toEntity(runner, runner.getId()));
            appendAssignmentIfScopeChanged(runner, saved.getId());
            return restoreRunner(saved);
        } catch (Exception e) {
            throw persistence("Database operation failed during save runner", e);
        }
    }

    @Override
    public void deleteById(Long runnerId) {
        try {
            runnerAssignmentJpaRepository.deleteByRunnerId(runnerId);
            runnerJpaRepository.deleteById(runnerId);
        } catch (Exception e) {
            throw persistence("Database operation failed during delete runner", e);
        }
    }

    @Override
    public Optional<Runner> findById(Long runnerId) {
        try {
            return runnerJpaRepository.findById(runnerId).map(this::restoreRunner);
        } catch (Exception e) {
            throw persistence("Database operation failed during find runner by id", e);
        }
    }

    @Override
    public Optional<Runner> findByToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }
            return runnerJpaRepository.findFirstByTokenOrderByIdDesc(token).map(this::restoreRunner);
        } catch (Exception e) {
            throw persistence("Database operation failed during find runner by token", e);
        }
    }

    @Override
    public List<Runner> findAll() {
        try {
            return runnerJpaRepository.findAll().stream().map(this::restoreRunner).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find all runners", e);
        }
    }

    /**
     * Records a new effective scope, and only a new one.
     *
     * <p>Append rather than update in place: reads already take the newest row by
     * {@code ASSIGNED_AT}, {@code RUNNER_ASSIGNMENT} carries no unique key on {@code RUNNER_ID}, and
     * an in-place update would first have to look up the newest row's id -- a query that does not
     * exist. Appending also leaves an audit trail of scope changes.
     *
     * <p>Only when the value differs. {@code activate} goes down this branch on every runner restart
     * and carries the scope through unchanged, so writing unconditionally would turn this table into
     * a restart log: unbounded growth, and a scope history in which almost no row is a scope change.
     */
    private void appendAssignmentIfScopeChanged(Runner runner, Long runnerId) {
        RunnerAssignmentJpaEntity current = runnerAssignmentJpaRepository
                .findFirstByRunnerIdOrderByAssignedAtDescIdDesc(runnerId)
                .orElse(null);
        String targetType = runner.getScopeType().name();
        Long targetId = runner.getScopeType().requiresTargetId() ? runner.getScopeTargetId() : null;
        if (current != null
                && targetType.equals(current.getTargetType())
                && Objects.equals(targetId, current.getTargetId())) {
            return;
        }
        runnerAssignmentJpaRepository.save(toAssignmentEntity(runner, runnerId));
    }

    private Runner restoreRunner(RunnerJpaEntity entity) {
        RunnerAssignmentJpaEntity assignment = runnerAssignmentJpaRepository
                .findFirstByRunnerIdOrderByAssignedAtDescIdDesc(entity.getId())
                .orElse(null);
        // A runner with no assignment row is GLOBAL, not an error. That is the MyBatis fallback and it
        // is load-bearing: a runner created before assignments existed must still dispatch.
        RunnerScopeType scopeType = assignment != null
                ? RunnerScopeType.valueOf(assignment.getTargetType())
                : RunnerScopeType.GLOBAL;
        Long targetId = assignment != null ? assignment.getTargetId() : null;
        return toDomain(entity, scopeType, targetId);
    }

    private RunnerJpaEntity toEntity(Runner runner, Long id) {
        RunnerJpaEntity entity = new RunnerJpaEntity();
        entity.setId(id);
        entity.setToken(runner.getToken());
        entity.setDescription(runner.getDescription());
        entity.setStatus(runner.getStatus().name());
        entity.setIpAddress(runner.getIpAddress());
        entity.setLastHeartbeatAt(runner.getLastHeartbeatAt());
        entity.setCreatedAt(runner.getCreatedAt());
        return entity;
    }

    private RunnerAssignmentJpaEntity toAssignmentEntity(Runner runner, Long runnerId) {
        RunnerAssignmentJpaEntity entity = new RunnerAssignmentJpaEntity();
        entity.setRunnerId(runnerId);
        entity.setTargetType(runner.getScopeType().name());
        // A GLOBAL scope has no target, and storing the caller's value anyway would let a stale id
        // leak into a scope that must ignore it. This mirrors the MyBatis mapper's guard.
        entity.setTargetId(runner.getScopeType().requiresTargetId() ? runner.getScopeTargetId() : null);
        entity.setAssignedAt(LocalDateTime.now());
        return entity;
    }

    private Runner toDomain(RunnerJpaEntity entity, RunnerScopeType scopeType, Long scopeTargetId) {
        return Runner.restore(
                entity.getId(),
                entity.getToken(),
                entity.getDescription(),
                RunnerStatus.valueOf(entity.getStatus()),
                scopeType,
                scopeTargetId,
                entity.getIpAddress(),
                entity.getLastHeartbeatAt(),
                entity.getCreatedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
