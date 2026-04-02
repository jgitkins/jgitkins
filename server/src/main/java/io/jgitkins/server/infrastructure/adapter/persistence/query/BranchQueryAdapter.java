package io.jgitkins.server.infrastructure.adapter.persistence.query;

import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.port.out.BranchQueryPort;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.persistence.mapper.BranchEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntity;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
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
