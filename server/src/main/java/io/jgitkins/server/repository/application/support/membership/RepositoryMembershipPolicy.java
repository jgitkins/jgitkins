package io.jgitkins.server.repository.application.support.membership;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.validate.RepositoryMemberValidator;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMembershipPolicy {

    private final RepositoryMemberValidator repositoryMemberValidator;

    public RepositoryMember createMember(RepositoryMemberAddCommand command) {
        repositoryMemberValidator.validateAddCommand(command);

        RepositoryId repositoryId = RepositoryId.of(command.repositoryId());
        UserId userId = UserId.of(command.userId());
        RepositoryMemberRole role = command.role() != null ? command.role() : RepositoryMemberRole.READER;

        return RepositoryMember.create(repositoryId, userId, role, null);
    }

    public boolean isAlreadyMember(RepositoryId repositoryId, UserId userId) {
        return repositoryMemberValidator.isAlreadyMember(repositoryId, userId);
    }

    public void validateMemberIdentifiers(Long repositoryId, Long userId) {
        repositoryMemberValidator.validateMemberIdentifiers(repositoryId, userId);
    }

    public void validateRepositoryId(Long repositoryId) {
        repositoryMemberValidator.validateRepositoryId(repositoryId);
    }
}
