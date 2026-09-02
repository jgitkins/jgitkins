package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMemberJpaPersistenceAdapter implements RepositoryMemberPersistencePort {

    private final RepositoryMemberJpaRepository repositoryMemberJpaRepository;

    @Override
    public boolean existsByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId) {
        try {
            if (repositoryId == null || userId == null) {
                return false;
            }
            return repositoryMemberJpaRepository
                    .existsByRepositoryIdAndUserId(repositoryId.getValue(), userId.getValue());
        } catch (Exception e) {
            throw persistence("Database operation failed during check repository member existence", e);
        }
    }

    @Override
    public Optional<RepositoryMember> findByRepositoryIdAndUserId(RepositoryId repositoryId,
                                                                 RepositoryMemberUserId userId) {
        try {
            if (repositoryId == null || userId == null) {
                return Optional.empty();
            }
            return repositoryMemberJpaRepository
                    .findFirstByRepositoryIdAndUserId(repositoryId.getValue(), userId.getValue())
                    .map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository member", e);
        }
    }

    @Override
    public RepositoryMember save(RepositoryMember member) {
        try {
            RepositoryMemberJpaEntity entity = new RepositoryMemberJpaEntity();
            entity.setRepositoryId(member.getRepositoryId().getValue());
            entity.setUserId(member.getUserId().getValue());
            entity.setRole(member.getRole().name());
            entity.setAddedAt(member.getAddedAt());
            return toDomain(repositoryMemberJpaRepository.save(entity));
        } catch (Exception e) {
            throw persistence("Database operation failed during save repository member", e);
        }
    }

    @Override
    public void deleteByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId) {
        try {
            repositoryMemberJpaRepository
                    .deleteByRepositoryIdAndUserId(repositoryId.getValue(), userId.getValue());
        } catch (Exception e) {
            throw persistence("Database operation failed during delete repository member", e);
        }
    }

    @Override
    public List<RepositoryMember> findAllByRepositoryId(RepositoryId repositoryId) {
        try {
            return repositoryMemberJpaRepository
                    .findAllByRepositoryIdOrderByAddedAtDesc(repositoryId.getValue())
                    .stream().map(this::toDomain).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find all repository members", e);
        }
    }

    private RepositoryMember toDomain(RepositoryMemberJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RepositoryMember.create(
                RepositoryId.of(entity.getRepositoryId()),
                RepositoryMemberUserId.of(entity.getUserId()),
                RepositoryMemberRole.valueOf(entity.getRole()),
                entity.getAddedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
