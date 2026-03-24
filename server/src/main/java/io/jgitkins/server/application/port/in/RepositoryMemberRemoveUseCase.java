package io.jgitkins.server.application.port.in;

public interface RepositoryMemberRemoveUseCase {
    void removeRepositoryMember(Long repositoryId, Long userId);
}
