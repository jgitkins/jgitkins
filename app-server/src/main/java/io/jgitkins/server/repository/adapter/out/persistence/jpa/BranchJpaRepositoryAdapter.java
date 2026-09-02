package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.domain.entity.Branch;
import io.jgitkins.server.repository.domain.repository.BranchRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA implementation of {@link BranchRepository}.
 *
 * <p>{@code delete} removes by {@code (repositoryId, name)} rather than by primary key, because that
 * is the branch's identity in the domain — {@code Branch} carries no id at all — and it is what the
 * MyBatis adapter's delete-by-condition did.
 */
@Component
@RequiredArgsConstructor
public class BranchJpaRepositoryAdapter implements BranchRepository {

    private final BranchJpaRepository branchJpaRepository;

    @Override
    public void save(Branch branch) {
        try {
            BranchJpaEntity entity = new BranchJpaEntity();
            entity.setRepositoryId(branch.getRepositoryId());
            entity.setName(branch.getName());
            entity.setLocked(branch.isLocked());
            entity.setCi(branch.isCiEnabled());
            entity.setDefaultBranch(branch.isDefaultBranch());
            branchJpaRepository.save(entity);
        } catch (Exception e) {
            throw persistence("Database operation failed during branch creation", e);
        }
    }

    @Override
    public void delete(Branch branch) {
        try {
            branchJpaRepository.deleteByRepositoryIdAndName(branch.getRepositoryId(), branch.getName());
        } catch (Exception e) {
            throw persistence("Database operation failed during branch delete", e);
        }
    }

    @Override
    public Optional<Branch> findByRepositoryIdAndName(Long repositoryId, String branchName) {
        try {
            return branchJpaRepository.findFirstByRepositoryIdAndName(repositoryId, branchName)
                    .map(entity -> Branch.rehydrate(entity.getRepositoryId(), entity.getName(),
                            entity.isLocked(), entity.isCi(), entity.isDefaultBranch()));
        } catch (Exception e) {
            throw persistence("Database operation failed during get branch", e);
        }
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
