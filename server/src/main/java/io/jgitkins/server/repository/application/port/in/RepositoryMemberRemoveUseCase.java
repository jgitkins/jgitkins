package io.jgitkins.server.repository.application.port.in;

public interface RepositoryMemberRemoveUseCase {
    void removeRepositoryMember(Long repositoryId, Long userId);
}
