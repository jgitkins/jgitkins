package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMemberValidator {

    private final RepositoryMemberPersistencePort repositoryMemberPort;

    // TODO: form 검증은 presentation 계층으로 이관할것
    public void validateAddCommand(RepositoryMemberAddCommand command) {
        if (command == null || command.getRepositoryId() == null || command.getUserId() == null) {
            throw new ApplicationException(
                    ApplicationErrorCode.MEMBER_IDENTIFIER_REQUIRED,
                    "RepositoryId and UserId are required to add a repository member");
        }
    }

    public void validateRepositoryId(Long repositoryId) {
        if (repositoryId == null) {
            throw new ApplicationException(
                    ApplicationErrorCode.MEMBER_IDENTIFIER_REQUIRED,
                    "RepositoryId is required");
        }
    }

    public void validateMemberIdentifiers(Long repositoryId, Long userId) {
        if (repositoryId == null || userId == null) {
            throw new ApplicationException(
                    ApplicationErrorCode.MEMBER_IDENTIFIER_REQUIRED,
                    "RepositoryId and UserId are required");
        }
    }

    public boolean isAlreadyMember(RepositoryId repositoryId, UserId userId) {
        return repositoryMemberPort.existsByRepositoryIdAndUserId(repositoryId, userId);
    }
}
