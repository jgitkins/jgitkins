package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of {@link RunnerRepository}.
 *
 * <p>A runner is stored across two tables: its own row, and an assignment row carrying the dispatch
 * scope. The effective scope is the newest assignment by {@code ASSIGNED_AT}, because
 * {@code RUNNER_ASSIGNMENT} has no unique key on {@code RUNNER_ID} and rows accumulate.
 *
 * <p><strong>Known defect, preserved deliberately.</strong> On the update branch, the MyBatis adapter
 * calls {@code updateByPrimaryKeySelective} with an assignment entity whose id was never populated —
 * {@code RunnerAssignmentDomainMapper.toEntity} does not map it — so the statement resolves to
 * {@code where ID = null} and updates nothing. Changing a runner's scope therefore has no effect today.
 * This adapter reproduces that no-op rather than fixing it, because the selector's contract is that
 * flipping it changes nothing observable: a JPA path that silently started honouring scope changes
 * would make the two providers disagree about a security-relevant decision, which is worse than the bug.
 * The fix belongs in its own task, against both providers at once. See
 * {@code RunnerJpaMariaDbIntegrationTest#scopeUpdateIsANoOpUnderBothProviders}, which pins the current
 * behaviour and says so.
 */
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
            // Intentionally not written: see the class javadoc. The MyBatis path issues an update
            // against a null assignment id and changes no row, so writing one here would diverge.
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

    private Runner restoreRunner(RunnerJpaEntity entity) {
        RunnerAssignmentJpaEntity assignment = runnerAssignmentJpaRepository
                .findFirstByRunnerIdOrderByAssignedAtDesc(entity.getId())
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
