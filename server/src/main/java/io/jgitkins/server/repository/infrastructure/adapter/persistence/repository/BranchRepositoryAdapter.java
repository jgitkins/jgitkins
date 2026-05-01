package io.jgitkins.server.repository.infrastructure.adapter.persistence.repository;

import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.repository.BranchRepository;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.BranchDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.BranchEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntity;
import io.jgitkins.server.infrastructure.persistence.model.BranchEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchRepositoryAdapter implements BranchRepository {

    private final BranchDomainMapper branchDomainMapper;
    private final BranchEntityMbgMapper branchEntityMbgMapper;

    @Override
    public void save(Branch branch) {
        try {
            BranchEntity branchEntity = branchDomainMapper.toEntity(branch);
            branchEntityMbgMapper.insertSelective(branchEntity);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during branch creation", e);
        }
    }

    @Override
    public void delete(Branch branch) {
        try {
            BranchEntityCondition condition = new BranchEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(branch.getRepositoryId())
                    .andNameEqualTo(branch.getName());
            branchEntityMbgMapper.deleteByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during branch delete", e);
        }
    }

    /***
     * query
     */
    @Override
    public Optional<Branch> findByRepositoryIdAndName(Long repositoryId, String branchName) {
        try {
            BranchEntityCondition condition = new BranchEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId)
                    .andNameEqualTo(branchName);

            return branchEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(branchDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, "Database operation failed during get branch", e);
        }
    }

}
