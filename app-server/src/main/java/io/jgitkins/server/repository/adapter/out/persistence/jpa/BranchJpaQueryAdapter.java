package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.application.contract.BranchSearchResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchJpaQueryAdapter implements BranchQueryPort {

    private final BranchJpaRepository branchJpaRepository;

    @Override
    public List<BranchSearchResult> findAllByRepositoryId(Long repositoryId) {
        try {
            return branchJpaRepository.findAllByRepositoryId(repositoryId)
                    .stream().map(this::toSearchResult).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during get branches", e);
        }
    }

    @Override
    public Optional<BranchSearchResult> findByRepositoryIdAndName(Long repositoryId, String branchName) {
        try {
            return branchJpaRepository.findFirstByRepositoryIdAndName(repositoryId, branchName)
                    .map(this::toSearchResult);
        } catch (Exception e) {
            throw persistence("Database operation failed during get branch", e);
        }
    }

    private BranchSearchResult toSearchResult(BranchJpaEntity entity) {
        return new BranchSearchResult(
                entity.getRepositoryId(),
                entity.getName(),
                entity.isLocked(),
                entity.isCi(),
                entity.isDefaultBranch());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
