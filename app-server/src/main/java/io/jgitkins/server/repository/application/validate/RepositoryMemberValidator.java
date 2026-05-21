package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.exception.MemberIdentifierRequiredException;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMemberValidator {

    private final RepositoryMemberPersistencePort repositoryMemberPort;

    // TODO: form 검증은 presentation 계층으로 이관할것
    public void validateAddCommand(RepositoryMemberAddCommand command) {
        if (command == null || command.repositoryId() == null || command.userId() == null) {
            throw new MemberIdentifierRequiredException(
                    "RepositoryId and UserId are required to add a repository member");
        }
    }

    public void validateRepositoryId(Long repositoryId) {
        if (repositoryId == null) {
            throw new MemberIdentifierRequiredException(
                    "RepositoryId is required");
        }
    }

    public void validateMemberIdentifiers(Long repositoryId, Long userId) {
        if (repositoryId == null || userId == null) {
            throw new MemberIdentifierRequiredException(
                    "RepositoryId and UserId are required");
        }
    }

    public boolean isAlreadyMember(RepositoryId repositoryId, UserId userId) {
        return repositoryMemberPort.existsByRepositoryIdAndUserId(repositoryId, userId);
    }
}
