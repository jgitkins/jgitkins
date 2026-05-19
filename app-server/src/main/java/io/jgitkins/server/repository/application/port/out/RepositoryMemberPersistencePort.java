package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.util.Optional;

public interface RepositoryMemberPersistencePort {
    RepositoryMember save(RepositoryMember member);

    boolean existsByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId);

    Optional<RepositoryMember> findByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId);

    void deleteByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId);

    java.util.List<RepositoryMember> findAllByRepositoryId(RepositoryId repositoryId);

}
