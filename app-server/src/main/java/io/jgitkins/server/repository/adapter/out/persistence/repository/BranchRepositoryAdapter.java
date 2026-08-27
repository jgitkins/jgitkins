package io.jgitkins.server.repository.adapter.out.persistence.repository;

import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.infrastructure.mapper.BranchDomainMapper;
import io.jgitkins.server.repository.infrastructure.persistence.mapper.BranchEntityMbgMapper;
import io.jgitkins.server.repository.infrastructure.persistence.model.BranchEntity;
import io.jgitkins.server.repository.infrastructure.persistence.model.BranchEntityCondition;
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
