package io.jgitkins.server.repository.adapter.out.persistence.query;

import io.jgitkins.server.repository.application.contract.BranchSearchResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.adapter.out.persistence.translator.BranchEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.model.BranchEntity;
import io.jgitkins.server.repository.adapter.out.persistence.model.BranchEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Registered by {@code RepositoryPersistenceSelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.72: with a JPA implementation of the
 * same port on the classpath, scanning would register two candidates and the injection point would
 * be ambiguous. The composition root now names exactly one.
 */
@RequiredArgsConstructor
public class BranchQueryAdapter implements BranchQueryPort {

    private final BranchEntityMbgMapper branchEntityMbgMapper;

    @Override
    public List<BranchSearchResult> findAllByRepositoryId(Long repositoryId) {
        try {
            BranchEntityCondition condition = new BranchEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId);

            return branchEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .map(this::toSearchResult)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during get branches",
                    e
            );
        }
    }

    @Override
    public Optional<BranchSearchResult> findByRepositoryIdAndName(Long repositoryId, String branchName) {
        try {
            BranchEntityCondition condition = new BranchEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId)
                    .andNameEqualTo(branchName);

            return branchEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(this::toSearchResult);
        } catch (Exception e) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during get branch",
                    e
            );
        }
    }

    private BranchSearchResult toSearchResult(BranchEntity entity) {
        return new BranchSearchResult(
                entity.getRepositoryId(),
                entity.getName(),
                Boolean.TRUE.equals(entity.getIsLocked()),
                Boolean.TRUE.equals(entity.getIsCi()),
                Boolean.TRUE.equals(entity.getIsDefault())
        );
    }
}
