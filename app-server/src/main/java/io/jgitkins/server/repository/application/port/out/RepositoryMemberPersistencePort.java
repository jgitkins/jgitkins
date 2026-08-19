package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import java.util.Optional;

public interface RepositoryMemberPersistencePort {
    RepositoryMember save(RepositoryMember member);

    boolean existsByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId);

    Optional<RepositoryMember> findByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId);

    void deleteByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId);

    java.util.List<RepositoryMember> findAllByRepositoryId(RepositoryId repositoryId);

}
